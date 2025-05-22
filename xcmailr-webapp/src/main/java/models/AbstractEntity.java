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
package models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.ebean.DB;


@MappedSuperclass
public abstract class AbstractEntity
{
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    // -------------------------------------
    // Getters and Setters
    // -------------------------------------
    /**
     * @return the ID of this MBox (primary key in the DB)
     */
    @JsonProperty("id")
    public long getId()
    {
        return id;
    }

    /**
     * @param id
     *            the ID of this MBox to set
     */
    @JsonIgnore
    public void setId(long id)
    {
        this.id = id;
    }

    /**
     * Deletes the object from the database
     */
    public void delete()
    {
        DB.delete(this);
    }

    /**
     * Stores the object in the Database
     */

    public void save()
    {
        DB.save(this);
    }

    /**
     * Updates the object in the DB
     */
    public void update()
    {
        DB.update(this);
    }

}
