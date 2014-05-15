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
package org.kuali.kfs.gl.batch.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.FileUtils;
import org.kuali.kfs.gl.batch.service.impl.IcrEncumbranceServiceImpl;
import org.kuali.kfs.sys.ConfigureContext;
import org.kuali.kfs.sys.businessobject.UniversityDate;
import org.kuali.kfs.sys.context.KualiTestBase;
import org.kuali.kfs.sys.context.ProxyUtils;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.service.UniversityDateService;
import org.kuali.rice.krad.util.ObjectUtils;


@ConfigureContext
public class IcrEncumbranceServiceTest extends KualiTestBase {

    private IcrEncumbranceService icrEncumbranceService;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        icrEncumbranceService = SpringContext.getBean(IcrEncumbranceService.class);
        icrEncumbranceService = (IcrEncumbranceServiceImpl) ProxyUtils.getTargetIfProxied(icrEncumbranceService);
        ((IcrEncumbranceServiceImpl)icrEncumbranceService).setUniversityDateService(new UniversityDateServiceDummy());
    }


    /**
     * This test runs several checks for file creation and correct contents of
     * the generated file.
     */
    public void testBuildIcrEncumbranceFeed(){
        File icrEncumbranceFeedFile = icrEncumbranceService.buildIcrEncumbranceFeed();
        assertTrue("The ICR Encumbrance file was found to be null.", ObjectUtils.isNotNull(icrEncumbranceFeedFile));
        assertTrue("The ICR Encumbrance file does not exist, should be at: " + icrEncumbranceFeedFile.getAbsolutePath(), icrEncumbranceFeedFile.exists());
        assertTrue("The ICR Encumbrance file should not be empty, located at: " + icrEncumbranceFeedFile.getAbsolutePath(), !isFileEmpty(icrEncumbranceFeedFile));
        FileUtils.delete(icrEncumbranceFeedFile);
    }

    private boolean isFileEmpty(File file){
        BufferedReader reader = null;
        String testLine = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            testLine = reader.readLine();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
            IOUtils.closeQuietly(reader);
        }

        return StringUtils.isBlank(testLine);
    }


    protected static class UniversityDateServiceDummy implements UniversityDateService{

        private static final Integer YEAR = 2009;
        private static final int MONTH = 3;
        private static final int DAY = 14;
        private static final Date DATE;
        static{
            SimpleDateFormat sdf = new SimpleDateFormat("YY-MM-dd");
            try {
                DATE = sdf.parse(String.format("%s-%s-%s", YEAR, MONTH, DAY));
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }


        public UniversityDateServiceDummy(){
            //
        }


        @Override
        public UniversityDate getCurrentUniversityDate() {
            UniversityDate universityDate = new UniversityDate();
            java.sql.Date sqlDate = new java.sql.Date(DATE.getTime());
            universityDate.setUniversityDate(sqlDate);
            return universityDate;
        }

        @Override
        public Integer getFiscalYear(Date date) {
            return YEAR;
        }

        @Override
        public Date getFirstDateOfFiscalYear(Integer fiscalYear) {
            throw new NotImplementedException();
        }

        @Override
        public Date getLastDateOfFiscalYear(Integer fiscalYear) {
            throw new NotImplementedException();
        }

        @Override
        public Integer getCurrentFiscalYear() {
            return YEAR;
        }

    }
}
