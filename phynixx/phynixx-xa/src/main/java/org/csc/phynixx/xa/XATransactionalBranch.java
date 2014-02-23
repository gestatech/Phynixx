package org.csc.phynixx.xa;

import org.csc.phynixx.common.exceptions.SampleTransactionalException;
import org.csc.phynixx.common.logger.IPhynixxLogger;
import org.csc.phynixx.common.logger.PhynixxLogManager;
import org.csc.phynixx.connection.*;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * an XID represents a transaction branch. A transaction branch represents the context a rollback/commit affects.
 * <p/>
 * A transactional branch corresponds to an physical connection
 * <p/>
 * Created by zf4iks2 on 10.02.14.
 */
class XATransactionalBranch<C extends IPhynixxConnection> extends PhynixxManagedConnectionListenerAdapter<C> implements IPhynixxManagedConnectionListener<C> {

    private static final IPhynixxLogger LOG = PhynixxLogManager.getLogger(XATransactionalBranch.class);

    private XAResourceProgressState progressState = XAResourceProgressState.ACTIVE;

    private boolean readOnly = true;

    private XAResourceActivationState activeState = XAResourceActivationState.ACTIVE;

    private volatile boolean rollbackOnly = false;

    private int heuristicState = 0;

    private Xid xid;

    private IPhynixxManagedConnection<C> managedConnection;

    XATransactionalBranch(Xid xid, IPhynixxManagedConnection<C> managedConnection) {
        this.xid = xid;
        this.managedConnection = managedConnection;
        this.managedConnection.addConnectionListener(this);
    }

    Xid getXid() {
        return xid;
    }

    IPhynixxManagedConnection<C> getManagedConnection() {
        return managedConnection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XATransactionalBranch that = (XATransactionalBranch) o;

        if (!xid.equals(that.xid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return xid.hashCode();
    }

    void suspend() {
        if (this.activeState == XAResourceActivationState.SUSPENDED) {
            return;
        }
        if (this.progressState != XAResourceProgressState.ACTIVE) {
            throw new IllegalStateException("A already prepared or preparing TX can not be suspended");

        }
        this.activeState = XAResourceActivationState.ACTIVE;
    }

    void resume() {
        if (this.activeState == XAResourceActivationState.SUSPENDED) {
            return;
        }
        this.activeState = XAResourceActivationState.ACTIVE;
    }

    public boolean isActive() {
        return this.getActiveState() == XAResourceActivationState.ACTIVE;
    }

    public boolean isXAProtocolFinished() {
        return !this.isProgressStateIn(XAResourceProgressState.ACTIVE);
    }

    boolean isSuspended() {
        return this.getActiveState() == XAResourceActivationState.SUSPENDED;
    }

    boolean isReadOnly() {
        return readOnly;
    }

    void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    private XAResourceActivationState getActiveState() {
        return activeState;
    }

    void setRollbackOnly(boolean rbOnly) {
        this.rollbackOnly = rbOnly;
    }

    boolean isRollbackOnly() {
        // new Exception("Read " + this.rollbackOnly+" ObjectId"+super.toString()).printStackTrace();
        return rollbackOnly;
    }

    XAResourceProgressState getProgressState() {
        return progressState;
    }

    private void setProgressState(XAResourceProgressState progressState) {
        this.progressState = progressState;
    }


    boolean hasHeuristicOutcome() {
        return (this.heuristicState > 0);
    }

    int getHeuristicState() {
        return this.heuristicState;
    }


    /**
     * maintains the state of the transactional branch if a commit is executed.
     * <p/>
     * The commit is executed
     *
     * @param onePhase
     * @throws XAException
     */
    void commit(boolean onePhase) throws XAException {
        if (this.getProgressState() == XAResourceProgressState.COMMITTED) {
            return;
        }

        if (this.hasHeuristicOutcome()) {
            throw new XAException(this.heuristicState);
        }
        if (!checkTXRequirements()) {
            throw new SampleTransactionalException("State not transactional " + this);
        }

        this.checkRollback();

        if (this.getProgressState() != XAResourceProgressState.ACTIVE && onePhase) {
            throw new XAException(XAException.XAER_RMFAIL);
        } else if (this.getProgressState() != XAResourceProgressState.PREPARED && !onePhase) {
            throw new XAException(XAException.XAER_RMFAIL);
        } else if (this.getProgressState() != XAResourceProgressState.PREPARED && this.getProgressState() != XAResourceProgressState.ACTIVE) {
            throw new XAException(XAException.XAER_RMFAIL);
        }

        this.setProgressState(XAResourceProgressState.COMMITTING);
        this.getManagedConnection().commit();
        this.setProgressState(XAResourceProgressState.COMMITTED);

    }

    private void checkRollback() throws XAException {
        if (this.getManagedConnection().getCoreConnection() == null ||
                this.getManagedConnection().isClosed()) {
            this.setProgressState(XAResourceProgressState.ROLLING_BACK);
            throw new XAException(XAException.XA_RBROLLBACK);
        }

        if (this.isRollbackOnly()) {
            this.setProgressState(XAResourceProgressState.ROLLING_BACK);
            throw new XAException(XAException.XA_RBROLLBACK);
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("RollbackOnly " + this.rollbackOnly);
        }
    }

    int prepare() throws XAException {
        // must find connection for this transaction
        if (!this.isProgressStateIn(XAResourceProgressState.ACTIVE)) {// must have had start() called
            LOG.error("XAResource " + this + " must have start() called ");
            throw new XAException(XAException.XAER_PROTO);
        }

        if (this.progressState == XAResourceProgressState.PREPARED) {
            if (this.isReadOnly()) {
                return (XAResource.XA_RDONLY);
            } else {
                return XAResource.XA_OK;
            }
        }

        // check if resource is READONLY -> no prepare is required
        if (this.isReadOnly()) {
            this.setProgressState(XAResourceProgressState.PREPARED);
            return (XAResource.XA_RDONLY);
        }


        if (this.progressState == XAResourceProgressState.PREPARING) {
            throw new IllegalStateException("XAResource already preparing");
        }
        if (!checkTXRequirements()) {
            throw new SampleTransactionalException("State not transactional " + this);
        }

        if (this.hasHeuristicOutcome()) {
            throw new XAException(this.heuristicState);
        }
        this.checkRollback();
        this.setProgressState(XAResourceProgressState.PREPARING);
        this.getManagedConnection().prepare();
        this.setProgressState(XAResourceProgressState.PREPARED);

        // anything to commit?
        if (this.isReadOnly()) {
            return (XAResource.XA_RDONLY);
        } else {
            return XAResource.XA_OK;
        }
    }


    private boolean isProgressStateIn(XAResourceProgressState... states) {
        if (states == null || states.length == 0) {
            return false;
        }
        XAResourceProgressState currentStates = this.getProgressState();
        for (int i = 0; i < states.length; i++) {
            if (states[i] == currentStates) {
                return true;
            }
        }
        return false;
    }


    void rollback() throws XAException {
        if (!checkTXRequirements()) {
            LOG.error("State not transactional " + this);
            throw new XAException(XAException.XAER_PROTO);
        }

        LOG.debug("XATransactionalBranch:rollback for xid=" + xid + " current Status=" + ConstantsPrinter.getStatusMessage(this.getProgressState()));
        switch (this.getProgressState()) {
            case PREPARED: // ready to do rollback
            case ACTIVE:
                try {
                    LOG.debug(
                            "XATransactionalBranch:rollback try to perform the rollback operation");
                    this.setProgressState(XAResourceProgressState.ROLLING_BACK);

                    // do the rollback
                    if (this.getManagedConnection() != null &&
                            !this.getManagedConnection().isClosed()) {
                        this.getManagedConnection().rollback();
                    } else {
                        LOG.info("XATransactionalBranch connection already closed -> no rollback");
                    }

                    this.setProgressState(XAResourceProgressState.ROLLEDBACK);
                    // perform the rollback operation
                    LOG.debug(
                            "XATransactionalBranch:rollback performed the rollback");
                } catch (Exception e) {
                    LOG.error("Error in " + this + " \n" + e.getMessage());
                    throw new XAException(XAException.XAER_PROTO);
                    // rollback will have been performed
                }
                break;
            default:
                LOG.error("XATransactionalBranch : rollback not permitted on TX state " + ConstantsPrinter.getStatusMessage(this.getProgressState()) + " XAResoureTXState=" + this);
                throw new XAException(XAException.XAER_RMFAIL);
        }


    }


    /**
     * checks, if the current state complies with the requirements of an active TX
     *
     * @return
     */
    private boolean checkTXRequirements() {
        if (this.getManagedConnection() == null) {
            return false;
        }
        if (!isActive()) {
            return false;
        }

        return true;
    }

    public void close() {
        this.getManagedConnection().close();
        this.setProgressState(XAResourceProgressState.CLOSED);
    }

    @Override
    public void connectionRequiresTransaction(IManagedConnectionProxyEvent<C> event) {
        this.setReadOnly(false);
    }

    @Override
    public void connectionReleased(IManagedConnectionProxyEvent<C> event) {
        event.getManagedConnection().removeConnectionListener(this);
    }

    @Override
    public void connectionFreed(IManagedConnectionProxyEvent<C> event) {
        event.getManagedConnection().removeConnectionListener(this);
    }


}
