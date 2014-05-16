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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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


/**
 * The test class tests the file generation of the IcrEncumbranceService.
 */
@ConfigureContext
public class IcrEncumbranceServiceTest extends KualiTestBase {

    // Based KFS 5.3 contrib branch demo data
    private static final String ICR_ENCUMBRANCE_TEST_DATA_FILE_PATH = "test/unit/src/org/kuali/kfs/gl/batch/fixture/gl_icrencmb.data.txt";

    // The service to be tested
    private IcrEncumbranceService icrEncumbranceService;


    /**
     * Sets the icrEncumbranceService, and also changes icrEncumbranceService's
     * UniversityDateService in order to line up with test data dates.
     *
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        icrEncumbranceService = SpringContext.getBean(IcrEncumbranceService.class);

        // If we have time, create new spring bean in spring-gl-test.xml and inject UniversityDateServiceDummy
        icrEncumbranceService = (IcrEncumbranceServiceImpl) ProxyUtils.getTargetIfProxied(icrEncumbranceService);
        ((IcrEncumbranceServiceImpl)icrEncumbranceService).setUniversityDateService(new UniversityDateServiceDummy());
    }


    /**
     * This test checks that the IcrEncumbranceService will:
     * 1.) Query the DB and produce a non-empty feed file.
     * 2.) Ensure that records in the file are what is expected
     */
    public void testBuildIcrEncumbranceFeed(){
        File icrEncumbranceFeedFile = icrEncumbranceService.buildIcrEncumbranceFeed();

        assertTrue("The ICR Encumbrance file was found to be null.", ObjectUtils.isNotNull(icrEncumbranceFeedFile));
        assertTrue("The ICR Encumbrance file does not exist, should be at: " + icrEncumbranceFeedFile.getAbsolutePath(), icrEncumbranceFeedFile.exists());
        assertFalse("The ICR Encumbrance file should not be empty, located at: " + icrEncumbranceFeedFile.getAbsolutePath(), isFileEmpty(icrEncumbranceFeedFile));
        assertTrue("Not all lines are present or generated correctly.", linesGeneratedCorrectly(icrEncumbranceFeedFile));

        FileUtils.delete(icrEncumbranceFeedFile);
    }


    /*
     * Ensure generated file contains all lines as expected.
     */
    private boolean linesGeneratedCorrectly(File generatedIcrEncumbranceFeedFile){

        // Pull the test data into a list
        List<String> expectedLines = null;
        try {
            expectedLines = IOUtils.readLines(new FileReader(ICR_ENCUMBRANCE_TEST_DATA_FILE_PATH));
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Pull the service generated lines, as assembled from DB queries and logic
        List<String> generatedLines = null;
        try {
            generatedLines = IOUtils.readLines(new FileReader(generatedIcrEncumbranceFeedFile));
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Place into set for easy comparison
        Set<String> expectedSet = new HashSet<String>();
        expectedSet.addAll(expectedLines);

        // Place into set for easy comparison
        Set<String> generatedSet = new HashSet<String>();
        generatedSet.addAll(generatedLines);

        // Check that all test data lines are present in service generated lines
        for(String line : expectedSet){
            if(!generatedSet.contains(line)){
                // The generated lines should contain all test data lines, this is a failure
                return false;
            }
        }

        // If we get here, the service generated all lines correctly
        return true;
    }


    /*
     * Checks that the input file has at least one non-blank line
     */
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


    /*
     * Creating inner class to act as dummy date service. This class will be set
     * as IcrEncumbranceServiceImpl's date service.
     */
    protected static class UniversityDateServiceDummy implements UniversityDateService{

        // 14-Mar-2009, period 09
        private static final Integer YEAR = 2009;
        private static final int MONTH = 3;
        private static final int DAY = 14;
        private static final String PERIOD = "09";
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
            universityDate.setUniversityFiscalAccountingPeriod(PERIOD);
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
