package co.kwest.www.callmanager.database;

import android.os.Process;

import androidx.lifecycle.LiveData;

import java.util.List;

import co.kwest.www.callmanager.database.entity.CGroup;
import co.kwest.www.callmanager.database.entity.CGroupAndItsContacts;
import co.kwest.www.callmanager.database.entity.Contact;
import co.kwest.www.callmanager.util.Utilities;

public class DataRepository {

    private static DataRepository sInstance;

    private final AppDatabase mDatabase;

    public static DataRepository getInstance(final AppDatabase database) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(database);
                }
            }
        }
        return sInstance;
    }

    private DataRepository(final AppDatabase database) {
        mDatabase = database;
    }

    // - Insert - //

    public long[] insertCGroups(CGroup... cGroup) {
        if (Utilities.isInUIThread()) {
            //TODO start in thread
            return null;
        } else {
            return mDatabase.getCGroupDao().insert(cGroup);
        }
    }

    public void insertContacts(List<Contact> contacts) {
        if (Utilities.isInUIThread()) {
            //TODO start in thread
        } else {
            mDatabase.getContactDao().insert(contacts);
        }
    }

    // - Update - //

    public void update(Contact... contacts) {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mDatabase.getContactDao().update(contacts);
        });
        thread.start();
    }

    // - Delete - //

    public void deleteContact(long contactId) {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mDatabase.getContactDao().deleteById(contactId);
        });
        thread.start();
    }

    public void deleteCGroup(long listId) {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mDatabase.getCGroupDao().deleteById(listId);
        });
        thread.start();
    }

    // - Query - //

    public LiveData<List<Contact>> getAllContacts() {
        return mDatabase.getContactDao().getAllContacts();
    }

    public LiveData<List<Contact>> getContactsInList(CGroup list) {
        return mDatabase.getContactDao().getContactsInList(list.getListId());
    }

    public LiveData<List<Contact>> getContactsInList(long listId) {
        return mDatabase.getContactDao().getContactsInList(listId);
    }

    public LiveData<List<CGroup>> getAllCGroups() {
        return mDatabase.getCGroupDao().getAllCGroups();
    }

    public LiveData<List<CGroup>> getCGroup(long listId) {
        return mDatabase.getCGroupDao().getCGroupById(listId);
    }

    public LiveData<List<CGroupAndItsContacts>> getAllCGroupsAndTheirContacts() {
        return mDatabase.getCGroupAndItsContactsDao().getAllCGroupsAndTheirContacts();
    }
}
