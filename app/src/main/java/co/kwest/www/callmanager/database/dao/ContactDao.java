package co.kwest.www.callmanager.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import co.kwest.www.callmanager.database.entity.Contact;

@Dao
public interface ContactDao {

    @Insert
    void insert(Contact... contacts);

    @Insert
    void insert(List<Contact> contacts);

    @Update
    void update(Contact... contacts);

    @Query("DELETE FROM contact_table")
    void deleteAll();

    @Query("DELETE FROM contact_table WHERE phone_numbers LIKE '%' || :phoneNumber || '%'")
    int deleteByPhoneNumber(String phoneNumber);

    @Query("DELETE FROM contact_table WHERE contact_id = :id")
    int deleteById(long id);

    @Query("SELECT * from contact_table WHERE phone_numbers LIKE '%' || :phoneNumber || '%'")
    LiveData<List<Contact>> getContactsByPhoneNumber(String phoneNumber);

    @Query("SELECT * from contact_table ORDER BY contact_id ASC")
    LiveData<List<Contact>> getAllContacts();

    @Query("SELECT * from contact_table WHERE list_id LIKE :listId")
    LiveData<List<Contact>> getContactsInList(long listId);
}
