/*
 * Copyright 2010 The Kuali Foundation.
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
package org.kuali.kfs.module.external.kc.service;

import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.kuali.kfs.integration.cg.dto.HashMapElement;
import org.kuali.kfs.integration.cg.dto.KcObjectCode;
import org.kuali.kfs.module.external.kc.KcConstants;

@WebService(name = KcConstants.ObjectCodeService.WEB_SERVICE_NAME,
            targetNamespace = KcConstants.KFS_NAMESPACE_URI)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL,
             parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public interface KcObjectCodeService {

    public KcObjectCode getObjectCode( @WebParam(name="universityFiscalYear") String universityFiscalYear,
            @WebParam(name="chartOfAccountsCode") String chartOfAccountsCode,
            @WebParam(name="financialObjectCode") String financialObjectCode);

    public List<KcObjectCode> lookupObjectCodes( @WebParam(name="searchCriteria") java.util.List <HashMapElement> searchCriteria);

}
