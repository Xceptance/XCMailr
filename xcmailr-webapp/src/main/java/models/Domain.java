/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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
package models;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Entity representing an allowed email domain registered in XCMailr.
 * Maps to the {@code register_domains} table.
 *
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Entity
@Table(name = "register_domains")
public class Domain extends AbstractEntity implements Serializable
{
    /** UID to serialize this object */
    private static final long serialVersionUID = 2659762572278339375L;

    /**
     * The domain name (e.g., "example.com" or "xcmailr.test").
     */
    @NotEmpty
    @Size(max = 255)
    @Column(name = "domainname", nullable = false, length = 255)
    private String domainname;

    /**
     * Default constructor required by JPA and serialization.
     */
    public Domain()
    {
        this.domainname = "";
    }

    /**
     * Constructs a new Domain entity with the given domain name.
     *
     * @param domainname the domain name to assign
     */
    public Domain(final String domainname)
    {
        this.domainname = domainname;
    }

    /**
     * @return the domain name string
     */
    public String getDomainname()
    {
        return domainname;
    }

    /**
     * Sets the domain name string.
     *
     * @param domainname the domain name to set
     */
    public void setDomainname(final String domainname)
    {
        this.domainname = domainname;
    }
}
