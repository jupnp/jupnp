/*
 * Copyright (C) 2011-2026 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package org.jupnp.controlpoint;

import java.util.List;

import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.UserConstants;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.protocol.ProtocolCreationException;
import org.jupnp.protocol.sync.SendingSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscribe and receive events from a service through GENA.
 * <p>
 * Usage example, establishing a subscription with a {@link org.jupnp.model.meta.Service}:
 * </p>
 * 
 * <pre>
 * SubscriptionCallback callback = new SubscriptionCallback(service, 600) { // Timeout in seconds
 *
 *     public void established(GENASubscription sub) {
 *         System.out.println("Established: " + sub.getSubscriptionId());
 *     }
 *
 *     public void failed(GENASubscription sub, UpnpResponse response, Exception e) {
 *         System.err.println(createDefaultFailureMessage(response, e));
 *     }
 *
 *     public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
 *         // Reason should be null, or it didn't end regularly
 *     }
 *
 *     public void eventReceived(GENASubscription sub) {
 *         System.out.println("Event: " + sub.getCurrentSequence().getValue());
 *         Map&lt;String, StateVariableValue&gt; values = sub.getCurrentValues();
 *         StateVariableValue status = values.get("Status");
 *         System.out.println("Status is: " + status.toString());
 *     }
 *
 *     public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
 *         System.out.println("Missed events: " + numberOfMissedEvents);
 *     }
 * };
 *
 * upnpService.getControlPoint().execute(callback);
 * </pre>
 *
 * @author Christian Bauer
 */
public abstract class SubscriptionCallback implements Runnable {

    protected Logger logger = LoggerFactory.getLogger(SubscriptionCallback.class);

    protected final Service service;
    protected final Integer requestedDurationSeconds;

    private ControlPoint controlPoint;
    private GENASubscription subscription;

    protected SubscriptionCallback(Service service) {
        this.service = service;
        this.requestedDurationSeconds = UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS;
    }

    protected SubscriptionCallback(Service service, int requestedDurationSeconds) {
        this.service = service;
        this.requestedDurationSeconds = requestedDurationSeconds;
    }

    public Service getService() {
        return service;
    }

    public synchronized ControlPoint getControlPoint() {
        return controlPoint;
    }

    public synchronized void setControlPoint(ControlPoint controlPoint) {
        this.controlPoint = controlPoint;
    }

    public synchronized GENASubscription getSubscription() {
        return subscription;
    }

    public synchronized void setSubscription(GENASubscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public synchronized void run() {
        if (getControlPoint() == null) {
            throw new IllegalStateException("Callback must be executed through ControlPoint");
        }

        if (getService() instanceof LocalService) {
            establishLocalSubscription((LocalService) service);
        } else if (getService() instanceof RemoteService) {
            establishRemoteSubscription((RemoteService) service);
        }
    }

    private void establishLocalSubscription(LocalService service) {

        if (getControlPoint().getRegistry().getLocalDevice(service.getDevice().getIdentity().getUdn(), false) == null) {
            logger.trace("Local device service is currently not registered, failing subscription immediately");
            failed(null, null, new IllegalStateException("Local device is not registered"));
            return;
        }

        // Local execution of subscription on local service re-uses the procedure and lifecycle that is
        // used for inbound subscriptions from remote control points on local services!
        // Except that it doesn't ever expire, we override the requested duration with Integer.MAX_VALUE!

        LocalGENASubscription localSubscription = null;
        try {
            localSubscription = new LocalGENASubscription(service, Integer.MAX_VALUE, List.of()) {

                public void failed(Exception e) {
                    synchronized (SubscriptionCallback.this) {
                        SubscriptionCallback.this.setSubscription(null);
                        SubscriptionCallback.this.failed(null, null, e);
                    }
                }

                @Override
                public void established() {
                    synchronized (SubscriptionCallback.this) {
                        SubscriptionCallback.this.setSubscription(this);
                        SubscriptionCallback.this.established(this);
                    }
                }

                @Override
                public void ended(CancelReason reason) {
                    synchronized (SubscriptionCallback.this) {
                        SubscriptionCallback.this.setSubscription(null);
                        SubscriptionCallback.this.ended(this, reason, null);
                    }
                }

                @Override
                public void eventReceived() {
                    synchronized (SubscriptionCallback.this) {
                        logger.trace("Local service state updated, notifying callback, sequence is: {}",
                                getCurrentSequence());
                        SubscriptionCallback.this.eventReceived(this);
                        incrementSequence();
                    }
                }
            };

            logger.trace("Local device service is currently registered, also registering subscription");
            getControlPoint().getRegistry().addLocalSubscription(localSubscription);

            logger.trace("Notifying subscription callback of local subscription availability");
            localSubscription.establish();

            logger.trace("Simulating first initial event for local subscription callback, sequence: {}",
                    localSubscription.getCurrentSequence());
            eventReceived(localSubscription);
            localSubscription.incrementSequence();

            logger.trace("Starting to monitor state changes of local service");
            localSubscription.registerOnService();

        } catch (Exception e) {
            logger.trace("Local callback creation failed", e);
            if (localSubscription != null) {
                getControlPoint().getRegistry().removeLocalSubscription(localSubscription);
            }
            failed(localSubscription, null, e);
        }
    }

    private void establishRemoteSubscription(RemoteService service) {
        RemoteGENASubscription remoteSubscription = new RemoteGENASubscription(service, requestedDurationSeconds) {

            @Override
            public void failed(UpnpResponse responseStatus) {
                synchronized (SubscriptionCallback.this) {
                    SubscriptionCallback.this.setSubscription(null);
                    SubscriptionCallback.this.failed(this, responseStatus, null);
                }
            }

            @Override
            public void established() {
                synchronized (SubscriptionCallback.this) {
                    SubscriptionCallback.this.setSubscription(this);
                    SubscriptionCallback.this.established(this);
                }
            }

            @Override
            public void ended(CancelReason reason, UpnpResponse responseStatus) {
                synchronized (SubscriptionCallback.this) {
                    SubscriptionCallback.this.setSubscription(null);
                    SubscriptionCallback.this.ended(this, reason, responseStatus);
                }
            }

            @Override
            public void eventReceived() {
                synchronized (SubscriptionCallback.this) {
                    SubscriptionCallback.this.eventReceived(this);
                }
            }

            @Override
            public void eventsMissed(int numberOfMissedEvents) {
                synchronized (SubscriptionCallback.this) {
                    SubscriptionCallback.this.eventsMissed(this, numberOfMissedEvents);
                }
            }

            @Override
            public void invalidMessage(UnsupportedDataException e) {
                synchronized (SubscriptionCallback.this) {
                    SubscriptionCallback.this.invalidMessage(this, e);
                }
            }
        };

        SendingSubscribe protocol;
        try {
            protocol = getControlPoint().getProtocolFactory().createSendingSubscribe(remoteSubscription);
        } catch (ProtocolCreationException e) {
            failed(subscription, null, e);
            return;
        }
        protocol.run();
    }

    public synchronized void end() {
        if (subscription == null) {
            return;
        }
        if (subscription instanceof LocalGENASubscription) {
            endLocalSubscription((LocalGENASubscription) subscription);
        } else if (subscription instanceof RemoteGENASubscription) {
            endRemoteSubscription((RemoteGENASubscription) subscription);
        }
    }

    private void endLocalSubscription(LocalGENASubscription subscription) {
        logger.trace("Removing local subscription and ending it in callback: {}", subscription);
        getControlPoint().getRegistry().removeLocalSubscription(subscription);
        subscription.end(null); // No reason, on controlpoint request
    }

    private void endRemoteSubscription(RemoteGENASubscription subscription) {
        logger.trace("Ending remote subscription: {}", subscription);
        getControlPoint().getConfiguration().getSyncProtocolExecutorService()
                .execute(getControlPoint().getProtocolFactory().createSendingUnsubscribe(subscription));
    }

    protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception) {
        failed(subscription, responseStatus, exception, createDefaultFailureMessage(responseStatus, exception));
    }

    /**
     * Called when establishing a local or remote subscription failed. To get a nice error message that
     * transparently detects local or remote errors use <tt>createDefaultFailureMessage()</tt>.
     *
     * @param subscription The failed subscription object, not very useful at this point.
     * @param responseStatus For a remote subscription, if a response was received at all, this is it, otherwise
     *            <tt>null</tt>.
     * @param exception For a local subscription and failed creation of a remote subscription protocol (before
     *            sending the subscribe request), any exception that caused the failure, otherwise <tt>null</tt>.
     * @param defaultMsg A user-friendly error message.
     * @see #createDefaultFailureMessage
     */
    protected abstract void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
            String defaultMsg);

    /**
     * Called when a local or remote subscription was successfully established.
     *
     * @param subscription The successful subscription.
     */
    protected abstract void established(GENASubscription subscription);

    /**
     * Called when a local or remote subscription ended, either on user request or because of a failure.
     *
     * @param subscription The ended subscription instance.
     * @param reason If the subscription ended regularly (through <tt>end()</tt>), this is <tt>null</tt>.
     * @param responseStatus For a remote subscription, if the cause implies a remopte response and it was
     *            received, this is it (e.g. renewal failure response).
     */
    protected abstract void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus);

    /**
     * Called when an event for an established subscription has been received.
     * <p>
     * Use the {@link org.jupnp.model.gena.GENASubscription#getCurrentValues()} method to obtain
     * the evented state variable values.
     * </p>
     *
     * @param subscription The established subscription with fresh state variable values.
     */
    protected abstract void eventReceived(GENASubscription subscription);

    /**
     * Called when a received event was out of sequence, indicating that events have been missed.
     * <p>
     * It's up to you if you want to react to missed events or if you (can) silently ignore them.
     * </p>
     * 
     * @param subscription The established subscription.
     * @param numberOfMissedEvents The number of missed events.
     */
    protected abstract void eventsMissed(GENASubscription subscription, int numberOfMissedEvents);

    /**
     * @param responseStatus The (HTTP) response or <code>null</code> if there was no response.
     * @param exception The exception or <code>null</code> if there was no exception.
     * @return A human-friendly error message.
     */
    public static String createDefaultFailureMessage(UpnpResponse responseStatus, Exception exception) {
        String message = "Subscription failed: ";
        if (responseStatus != null) {
            message = message + " HTTP response was: " + responseStatus.getResponseDetails();
        } else if (exception != null) {
            message = message + " Exception occurred: " + exception;
        } else {
            message = message + " No response received.";
        }
        return message;
    }

    /**
     * Called when a received event message could not be parsed successfully.
     * <p>
     * This typically indicates a broken device which is not UPnP compliant. You can
     * react to this failure in any way you like, for example, you could terminate
     * the subscription or simply create an error report/log.
     * </p>
     * <p>
     * The default implementation will log the exception at <code>INFO</code> level, and
     * the invalid XML at <code>TRACE</code> level.
     * </p>
     *
     * @param remoteGENASubscription The established subscription.
     * @param e Call {@link org.jupnp.model.UnsupportedDataException#getData()} to access the invalid XML.
     */
    protected void invalidMessage(RemoteGENASubscription remoteGENASubscription, UnsupportedDataException e) {
        logger.info("Invalid event message received", e);
        if (logger.isTraceEnabled()) {
            logger.trace("------------------------------------------------------------------------------");
            logger.trace(e.getData() != null ? e.getData().toString() : "null");
            logger.trace("------------------------------------------------------------------------------");
        }
    }

    @Override
    public String toString() {
        return "(SubscriptionCallback) " + getService();
    }
}
