package co.kwest.www.callmanager.ui.activity;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import co.kwest.www.callmanager.database.AppDatabase;
import co.kwest.www.callmanager.database.DataRepository;
import co.kwest.www.callmanager.database.entity.CGroup;
import co.kwest.www.callmanager.database.entity.Contact;

public class CGroupViewModel extends AndroidViewModel {

    private DataRepository mRepository;

    private long mListId;
    private LiveData<List<Contact>> mContacts;

    /**
     * Constructor
     *
     * @param application
     */
    public CGroupViewModel(@NonNull Application application) {
        super(application);
        mRepository = DataRepository.getInstance(AppDatabase.getDatabase(application.getApplicationContext()));
    }

    /**
     * Sets the list id by a given long
     *
     * @param listId
     */
    public void setListId(long listId) {
        mListId = listId;
        mContacts = mRepository.getContactsInList(listId);
    }

    /**
     * Returns a list of the contact
     *
     * @return
     */
    public LiveData<List<Contact>> getContacts() {
        return mContacts;
    }

    /**
     * Returns a list of the CGroup
     *
     * @return
     */
    public LiveData<List<CGroup>> getCGroup() {
        return mRepository.getCGroup(mListId);
    }
}
