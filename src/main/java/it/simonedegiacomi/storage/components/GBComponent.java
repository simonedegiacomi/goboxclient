package it.simonedegiacomi.storage.components;

import it.simonedegiacomi.storage.StorageEnvironment;

/**
 * Interface that every gobox component must implement.
 *
 * Created on 25/05/16.
 * @author Degiacomi Simone
 */
public interface GBComponent {

    /**
     * This method is called when the component is activated in the storage
     * @param env Environment
     */
    void onAttach (StorageEnvironment env, ComponentConfig componentConfig) throws AttachFailException;

    /**
     * This method is called when the component is detached from the storage (for example at the shutdown)
     */
    void onDetach ();
}
