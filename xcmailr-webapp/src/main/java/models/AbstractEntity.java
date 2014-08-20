package models;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.avaje.ebean.Ebean;

@MappedSuperclass
public abstract class AbstractEntity
{
    @Id
    private long id;

    // -------------------------------------
    // Getters and Setters
    // -------------------------------------
    /**
     * @return the ID of this MBox (primary key in the DB)
     */
    public long getId()
    {
        return id;
    }

    /**
     * @param id
     *            the ID of this MBox to set
     */
    public void setId(long id)
    {
        this.id = id;
    }
    

    /**
     * Deletes the object from the database
     */
    public void delete()
    {
        Ebean.delete(this);
    }
    /**
     * Stores the object in the Database
     */

    public void save()
    {
        Ebean.save(this);
    }

    /**
     * Updates the object in the DB
     */
    public void update()
    {
        Ebean.update(this);
    }

}
