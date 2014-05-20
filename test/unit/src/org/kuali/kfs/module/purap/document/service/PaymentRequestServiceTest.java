/*
 * Copyright 2007 The Kuali Foundation
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
package org.kuali.kfs.module.purap.document.service;

import java.lang.reflect.Method;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.module.purap.PurapConstants;
import org.kuali.kfs.module.purap.PurapParameterConstants;
import org.kuali.kfs.module.purap.document.PaymentRequestDocument;
import org.kuali.kfs.module.purap.document.PaymentRequestDocumentTest;
import org.kuali.kfs.module.purap.document.PurchaseOrderDocument;
import org.kuali.kfs.module.purap.document.dataaccess.PaymentRequestDao;
import org.kuali.kfs.module.purap.document.service.impl.PaymentRequestServiceImpl;
import org.kuali.kfs.module.purap.fixture.PaymentRequestDocumentFixture;
import org.kuali.kfs.module.purap.fixture.PurchaseOrderDocumentFixture;
import org.kuali.kfs.sys.ConfigureContext;
import org.kuali.kfs.sys.context.KualiTestBase;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.fixture.UserNameFixture;
import org.kuali.rice.core.api.datetime.DateTimeService;
import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.kuali.rice.coreservice.framework.parameter.ParameterService;
import org.kuali.rice.kew.api.exception.WorkflowException;
import org.kuali.rice.krad.document.Document;
import org.kuali.rice.krad.service.DocumentService;

@ConfigureContext(session = UserNameFixture.appleton)
public class PaymentRequestServiceTest extends KualiTestBase {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PaymentRequestServiceTest.class);

    private DocumentService documentService;
    private KualiDecimal defaultMinimumLimit;
    private ParameterService parameterService;
    private NegativePaymentRequestApprovalLimitService npras;
    private PaymentRequestService paymentRequestService;
    private PaymentRequestDocumentTest preqDocTest;
    private PaymentRequestDao paymentRequestDao;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (null == documentService) {
            documentService = SpringContext.getBean(DocumentService.class);
        }
        if (null == parameterService) {
            parameterService = SpringContext.getBean(ParameterService.class);
            String samt = parameterService.getParameterValueAsString(PaymentRequestDocument.class, PurapParameterConstants.PURAP_DEFAULT_NEGATIVE_PAYMENT_REQUEST_APPROVAL_LIMIT);
            defaultMinimumLimit = new KualiDecimal(samt);
        }
        if (null == npras) {
            npras = SpringContext.getBean(NegativePaymentRequestApprovalLimitService.class);
        }
        if (null == paymentRequestService) {
            paymentRequestService = SpringContext.getBean(PaymentRequestService.class);
        }
        if (null == preqDocTest) {
            preqDocTest = new PaymentRequestDocumentTest();
        }
        if (null == paymentRequestDao) {
            paymentRequestDao = SpringContext.getBean(PaymentRequestDao.class);;
        }

    }

    private void cancelDocument(Document document) throws WorkflowException {
        documentService.cancelDocument(document, "testing complete");
    }

    private PaymentRequestDocument createBasicDocument() throws Exception {
        return createBasicDocument(preqDocTest.createPurchaseOrderDocument(PurchaseOrderDocumentFixture.PO_APPROVAL_REQUIRED,false));
    }

    private PaymentRequestDocument createBasicDocument(PurchaseOrderDocument po) throws Exception {
        PaymentRequestDocument preq = preqDocTest.createPaymentRequestDocument(
                PaymentRequestDocumentFixture.PREQ_APPROVAL_REQUIRED,
                po,
                true, new KualiDecimal[] {new KualiDecimal(100)});

        return preq;
    }

    public void testGetPaymentRequestDocNumberForAutoApprove() throws Exception {
        Date todayAtMidnight = SpringContext.getBean(DateTimeService.class).getCurrentSqlDateMidnight();

        PaymentRequestDocument preqShouldAutoApprove = createBasicDocument();
        preqShouldAutoApprove.setApplicationDocumentStatus(PurapConstants.PaymentRequestStatuses.APPDOC_AWAITING_FISCAL_REVIEW);
        preqShouldAutoApprove.setPaymentRequestPayDate(todayAtMidnight);
        documentService.saveDocument(preqShouldAutoApprove);
        String docIdShouldAutoApprove = preqShouldAutoApprove.getDocumentNumber();

        PaymentRequestDocument preqShouldNotAutoApprove = createBasicDocument();
        preqShouldNotAutoApprove.setApplicationDocumentStatus(PurapConstants.PaymentRequestStatuses.APPDOC_IN_PROCESS);
        preqShouldNotAutoApprove.setPaymentRequestPayDate(todayAtMidnight);
        documentService.saveDocument(preqShouldNotAutoApprove);
        String docIdShouldNotAutoApprove = preqShouldNotAutoApprove.getDocumentNumber();

        PaymentRequestServiceImpl prsi = new PaymentRequestServiceImpl();
        prsi.setDateTimeService(SpringContext.getBean(DateTimeService.class));
        prsi.setPaymentRequestDao(SpringContext.getBean(PaymentRequestDao.class));

        Method method = PaymentRequestServiceImpl.class.getDeclaredMethod("getPaymentRequestDocNumberForAutoApprove", null);
        method.setAccessible(true);
        List<String> docIds = (List<String>) method.invoke(prsi, null);


        assertTrue(docIds.contains(docIdShouldAutoApprove));
        assertTrue(!docIds.contains(docIdShouldNotAutoApprove));
    }

    public void testGetPaymentRequestsByStatusAndPurchaseOrderId() throws Exception {

        Map <String, String> result = paymentRequestService.getPaymentRequestsByStatusAndPurchaseOrderId(
                PurapConstants.PaymentRequestStatuses.APPDOC_IN_PROCESS, 0);
        assertEquals("N", result.get("hasInProcess"));
        assertEquals("N", result.get("checkInProcess"));

        PaymentRequestDocument preq2 = createBasicDocument();
        preq2.setApplicationDocumentStatus(PurapConstants.PaymentRequestStatuses.APPDOC_IN_PROCESS);
        documentService.saveDocument(preq2);

        result = paymentRequestService.getPaymentRequestsByStatusAndPurchaseOrderId(
                PurapConstants.PaymentRequestStatuses.APPDOC_IN_PROCESS, preq2.getPurchaseOrderIdentifier());
        assertEquals("Y", result.get("hasInProcess"));
        assertEquals("N", result.get("checkInProcess"));

        PaymentRequestDocument preq1 = createBasicDocument(preq2.getPurchaseOrderDocument());
        preq1.setApplicationDocumentStatus(PurapConstants.PaymentRequestStatuses.APPDOC_AWAITING_FISCAL_REVIEW);
        documentService.saveDocument(preq1);

        result = paymentRequestService.getPaymentRequestsByStatusAndPurchaseOrderId(
                PurapConstants.PaymentRequestStatuses.APPDOC_IN_PROCESS, preq2.getPurchaseOrderIdentifier());
        assertEquals("Y", result.get("hasInProcess"));
        assertEquals("Y", result.get("checkInProcess"));

        preq2.setApplicationDocumentStatus(PurapConstants.PaymentRequestStatuses.APPDOC_AWAITING_FISCAL_REVIEW);
        documentService.saveDocument(preq2);

        result = paymentRequestService.getPaymentRequestsByStatusAndPurchaseOrderId(
                PurapConstants.PaymentRequestStatuses.APPDOC_IN_PROCESS, preq2.getPurchaseOrderIdentifier());
        assertEquals("N", result.get("hasInProcess"));
        assertEquals("Y", result.get("checkInProcess"));
    }

}

