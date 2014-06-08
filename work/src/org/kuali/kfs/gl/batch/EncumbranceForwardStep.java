/*
 * Copyright 2006 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.gl.batch;

import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.gl.GeneralLedgerConstants;
import org.kuali.kfs.gl.batch.service.YearEndService;
import org.kuali.kfs.sys.batch.AbstractWrappedBatchStep;
import org.kuali.kfs.sys.batch.service.WrappedBatchExecutorService.CustomBatchExecutor;
import org.kuali.kfs.sys.service.impl.KfsParameterConstants;
import org.kuali.rice.krad.util.ObjectUtils;
import org.springframework.util.StopWatch;

/**
 * A step to run the year end process of forwarding encumbrances into the next fiscal year
 */
public class EncumbranceForwardStep extends AbstractWrappedBatchStep {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(EncumbranceForwardStep.class);
    private YearEndService yearEndService;

    public static final String TRANSACTION_DATE_FORMAT_STRING = "yyyy-MM-dd";

    /**
     * @see org.kuali.kfs.sys.batch.AbstractWrappedBatchStep#getCustomBatchExecutor()
     */
    @Override
    protected CustomBatchExecutor getCustomBatchExecutor() {
        return new CustomBatchExecutor() {
            /**
             * This step runs the forward encumbrance process, including retrieving the parameters needed to run the job, creating the
             * origin entry group where output origin entries will go, and having the job's reports generated.
             *
             * @return true if the step completed successfully, false if otherwise
             * @see org.kuali.kfs.sys.batch.Step#performStep()
             */
            @Override
            public boolean execute() {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start("EncumbranceForwardStep");

                Map jobParameters = new HashMap();
                Integer varFiscalYear = null;
                Date varTransactionDate = null;
                List<String> varCharts = null;

                String FIELD_FISCAL_YEAR = GeneralLedgerConstants.ColumnNames.UNIVERSITY_FISCAL_YEAR;
                String FIELD_TRANSACTION_DATE = GeneralLedgerConstants.ColumnNames.TRANSACTION_DT;

                // Get the current fiscal year.
                varFiscalYear = new Integer(getParameterService().getParameterValueAsString(KfsParameterConstants.GENERAL_LEDGER_BATCH.class, GeneralLedgerConstants.ANNUAL_CLOSING_FISCAL_YEAR_PARM));

                // Get the current date (transaction date).
                try {
                    DateFormat transactionDateFormat = new SimpleDateFormat(TRANSACTION_DATE_FORMAT_STRING);
                    varTransactionDate = new Date(transactionDateFormat.parse(getParameterService().getParameterValueAsString(KfsParameterConstants.GENERAL_LEDGER_BATCH.class, GeneralLedgerConstants.ANNUAL_CLOSING_TRANSACTION_DATE_PARM)).getTime());
                }
                catch (ParseException pe) {
                    LOG.error("Failed to parse TRANSACTION_DT from kualiConfigurationService");
                    throw new RuntimeException("Unable to get transaction date from kualiConfigurationService", pe);
                }

                //Obtain list of charts to close from Parameter ANNUAL_CLOSING_CHARTS_PARAM.
                //If no parameter value exists, act on all charts which is the default action in the delivered foundation code.
                varCharts = new ArrayList<String>();
                try {
                    String[] varChartsArray = getParameterService().getParameterValuesAsString(KfsParameterConstants.GENERAL_LEDGER_BATCH.class, GeneralLedgerConstants.ANNUAL_CLOSING_CHARTS_PARAM).toArray(new String[] {});

                    if (ObjectUtils.isNotNull(varChartsArray) && (varChartsArray.length != 0)) {
                        //transfer charts from parameter to List for database query
                        for (String chartParam : varChartsArray) {
                            varCharts.add(chartParam);
                        }
                        LOG.info("EncumbranceForwardJob ANNUAL_CLOSING_CHARTS parameter value = " + varCharts.toString());
                    }
                    else {
                        //Parameter existed but no values were listed.  Act on all charts which is the default action in the delivered foundation code.
                        LOG.info("ANNUAL_CLOSING_CHARTS parameter defined for KFS-GL Batch but no values were specified. All charts will be acted upon for EncumbranceForwardJob.");
                    }
                }
                catch (IllegalArgumentException e) {
                    //parameter is not defined, act on all charts per foundation delivered code
                    LOG.info("ANNUAL_CLOSING_CHARTS parameter was not defined for KFS-GL Batch. All charts will be acted upon for EncumbranceForwardJob.");
                }

                jobParameters.put(GeneralLedgerConstants.ColumnNames.UNIVERSITY_FISCAL_YEAR, varFiscalYear);
                jobParameters.put(GeneralLedgerConstants.ColumnNames.UNIV_DT, varTransactionDate);
                jobParameters.put(GeneralLedgerConstants.ColumnNames.CHART_OF_ACCOUNTS_CODE, varCharts);

                String encumbranceForwardFileName = GeneralLedgerConstants.BatchFileSystem.ENCUMBRANCE_FORWARD_FILE + GeneralLedgerConstants.BatchFileSystem.EXTENSION;
                Map<String, Integer> forwardEncumbranceCounts = new HashMap<String, Integer>();

                yearEndService.forwardEncumbrances(encumbranceForwardFileName, jobParameters, forwardEncumbranceCounts);

                stopWatch.stop();
                LOG.info("EncumbranceForwardStep took " + (stopWatch.getTotalTimeSeconds() / 60.0) + " minutes to complete");

                return true;
            }
        };
    }

    /**
     * Sets the yearEndService attribute, allowing the injection of an implementation of that service
     *
     * @param yearEndService the yearEndService to set
     * @see org.kuali.module.gl.service.YearEndService
     */
    public void setYearEndService(YearEndService yearEndService) {
        this.yearEndService = yearEndService;
    }
}
