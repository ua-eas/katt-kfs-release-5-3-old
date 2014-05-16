/*
 * Copyright 2014 The Kuali Foundation.
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

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.kuali.kfs.gl.GeneralLedgerConstants;
import org.kuali.kfs.gl.batch.service.IcrEncumbranceService;
import org.kuali.kfs.gl.batch.service.impl.IcrEncumbranceServiceImpl;
import org.kuali.kfs.sys.ConfigureContext;
import org.kuali.kfs.sys.context.KualiTestBase;
import org.kuali.kfs.sys.context.ProxyUtils;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.fixture.UniversityDateServiceFixture;
import org.kuali.rice.core.api.config.property.ConfigurationService;
import org.kuali.rice.core.api.datetime.DateTimeService;

/**
 * A class to test functionality of the IcrEncumbranceSortStepTest class.
 */
@ConfigureContext
public class IcrEncumbranceSortStepTest extends KualiTestBase {

    private IcrEncumbranceSortStep icrEncumbranceSortStep;
    private IcrEncumbranceService icrEncumbranceService;
    private DateTimeService dateTimeService;
    private ConfigurationService kualiConfigurationService;

    /**
     * Setup services used in test.
     *
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Init services
        icrEncumbranceSortStep = SpringContext.getBean(IcrEncumbranceSortStep.class);
        icrEncumbranceService = SpringContext.getBean(IcrEncumbranceService.class);
        dateTimeService = SpringContext.getBean(DateTimeService.class);
        kualiConfigurationService = SpringContext.getBean(ConfigurationService.class);

        // If we have time, see if it is possible to inject the date service via Spring config (spring-gl-test.xml)
        icrEncumbranceService = (IcrEncumbranceServiceImpl) ProxyUtils.getTargetIfProxied(icrEncumbranceService);
        ((IcrEncumbranceServiceImpl)icrEncumbranceService).setUniversityDateService(UniversityDateServiceFixture.DATE_2009_03_14.createUniversityDateService());
    }

    /**
     * Test to ensure IcrEncumbranceSortStep is performing file i/o correctly,
     * and that at the very least is not dropping or dupe'ing records.
     */
    public void testExecute(){

        // Create an input file via the related service
        File inputFile = icrEncumbranceService.buildIcrEncumbranceFeed();

        // Perform work to be tested
        icrEncumbranceSortStep.execute("testIcrEncumbranceSortStep", dateTimeService.getCurrentDate());

        // Grab the lines from the input file
        List<String> inputLines = null;
        try {
            inputLines = IOUtils.readLines(new FileReader(inputFile));
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Grab the lines of sorted file
        String outputFilePath = kualiConfigurationService.getPropertyValueAsString("staging.directory") + "/gl/originEntry" + File.separator + GeneralLedgerConstants.BatchFileSystem.ICR_ENCUMBRANCE_POSTER_INPUT_FILE + GeneralLedgerConstants.BatchFileSystem.EXTENSION;
        List<String> outputLines = null;
        try {
            outputLines = IOUtils.readLines(new FileReader(outputFilePath));
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Ensure line counts match
        int inputLineCount = inputLines.size();
        int outputLineCount = outputLines.size();
        assertTrue("There should not be a mismatch of line counts between input and output files: input.size(): " + inputLineCount + ", output.size(): " + outputLineCount, inputLineCount == outputLineCount);

    }

}
