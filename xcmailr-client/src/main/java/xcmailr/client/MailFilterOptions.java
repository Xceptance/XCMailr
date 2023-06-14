/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xcmailr.client;

/**
 * The filter configuration that can be passed into {@link MailApi#listMails(String, MailFilterOptions)}. Use this class
 * to specify filter criteria in order to reduce the number of mails returned from the server. Simply provide regular
 * expression patterns that will be applied to the respective field when filtering mails on the server. Only those mails
 * matching <em>all</em> specified criteria will be returned.
 * 
 * @see MailApi
 */
public class MailFilterOptions
{
    /**
     * A regular expression for the sender of the mail.
     */
    public String senderPattern;

    /**
     * A regular expression for the subject of the mail.
     */
    public String subjectPattern;

    /**
     * A regular expression for the headers of the mail.
     */
    public String headersPattern;

    /**
     * A regular expression for the HTML content of the mail.
     */
    public String htmlContentPattern;

    /**
     * A regular expression for the text content of the mail.
     */
    public String textContentPattern;

    /**
     * Whether or not to return just the most recent matching mail instead of the whole list of matches.
     */
    public boolean lastMatchOnly;

    /**
     * Sets a regular expression for the sender of the mail.
     */
    public MailFilterOptions senderPattern(final String senderPattern)
    {
        this.senderPattern = senderPattern;
        return this;
    }

    /**
     * Sets a regular expression for the subject of the mail.
     */
    public MailFilterOptions subjectPattern(final String subjectPattern)
    {
        this.subjectPattern = subjectPattern;
        return this;
    }

    /**
     * Sets a regular expression for the headers of the mail.
     */
    public MailFilterOptions headersPattern(final String headersPattern)
    {
        this.headersPattern = headersPattern;
        return this;
    }

    /**
     * Sets a regular expression for the HTML content of the mail.
     */
    public MailFilterOptions htmlContentPattern(final String htmlContentPattern)
    {
        this.htmlContentPattern = htmlContentPattern;
        return this;
    }

    /**
     * Sets a regular expression for the text content of the mail.
     */
    public MailFilterOptions textContentPattern(final String textContentPattern)
    {
        this.textContentPattern = textContentPattern;
        return this;
    }

    /**
     * Whether or not to return just the most recent matching mail instead of the whole list of matches.
     */
    public MailFilterOptions lastMatchOnly(final boolean lastMatchOnly)
    {
        this.lastMatchOnly = lastMatchOnly;
        return this;
    }
}
