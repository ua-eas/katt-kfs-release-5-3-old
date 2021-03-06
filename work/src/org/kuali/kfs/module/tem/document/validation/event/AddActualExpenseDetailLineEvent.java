/*
 * Copyright 2011 The Kuali Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.tem.document.validation.event;

import org.kuali.kfs.module.tem.businessobject.AbstractExpense;
import org.kuali.kfs.sys.document.validation.event.AttributedDocumentEventBase;
import org.kuali.rice.krad.document.Document;

/**
 * Event triggered when an {@link ActualExpense} instance
 * is added to a {@link Document}
 */
public class AddActualExpenseDetailLineEvent<E extends AbstractExpense> extends AttributedDocumentEventBase implements TemExpenseLineEvent<E> {

    private final E actualExpense;
    private final E actualExpenseDetail;

    /**
     * Constructs an AddExpenseLineEvent with the given errorPathPrefix, document, and otherExpense.
     *
     * @param errorPathPrefix
     * @param document
     * @param groupTraveler
     */
    public AddActualExpenseDetailLineEvent(String errorPathPrefix, Document document, E actualExpense, E actualExpenseDetail) {
        super("adding actualExpenseLine to document " + getDocumentId(document), errorPathPrefix, document);
        this.actualExpense = actualExpense;
        this.actualExpenseDetail = actualExpenseDetail;
    }

    @Override
    public E getExpenseLine() {
        return actualExpense;
    }

    public E getActualExpenseDetail() {
        return actualExpenseDetail;
    }
}
