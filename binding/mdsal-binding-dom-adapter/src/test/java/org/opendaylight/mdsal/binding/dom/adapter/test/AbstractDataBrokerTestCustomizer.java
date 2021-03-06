/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.binding.dom.adapter.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.adapter.test.util.MockSchemaService;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractDataBrokerTestCustomizer {

    private DOMDataBroker domDataBroker;
    private final DOMNotificationRouter domNotificationRouter;
    private final MockSchemaService schemaService;
    private ImmutableMap<LogicalDatastoreType, DOMStore> datastores;
    private final BindingToNormalizedNodeCodec bindingToNormalized;

    public ImmutableMap<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION,createConfigurationDatastore())
                .build();
    }

    public AbstractDataBrokerTestCustomizer() {
        this.schemaService = new MockSchemaService();
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry();
        final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        this.bindingToNormalized = new BindingToNormalizedNodeCodec(loading, codecRegistry);
        this.schemaService.registerSchemaContextListener(this.bindingToNormalized);
        this.domNotificationRouter = DOMNotificationRouter.create(16);
    }

    public DOMStore createConfigurationDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", getDataTreeChangeListenerExecutor());
        this.schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMStore createOperationalDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", getDataTreeChangeListenerExecutor());
        this.schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMDataBroker createDOMDataBroker() {
        return new SerializedDOMDataBroker(getDatastores(), getCommitCoordinatorExecutor());
    }

    public NotificationService createNotificationService() {
        return new BindingDOMNotificationServiceAdapter(this.domNotificationRouter,
                this.bindingToNormalized.getCodecRegistry());
    }

    public NotificationPublishService createNotificationPublishService() {
        return new BindingDOMNotificationPublishServiceAdapter(this.domNotificationRouter, this.bindingToNormalized);
    }

    public abstract ListeningExecutorService getCommitCoordinatorExecutor();

    public ListeningExecutorService getDataTreeChangeListenerExecutor() {
        return MoreExecutors.newDirectExecutorService();
    }

    public DataBroker createDataBroker() {
        return new BindingDOMDataBrokerAdapter(getDOMDataBroker(), this.bindingToNormalized);
    }

    public BindingToNormalizedNodeCodec getBindingToNormalized() {
        return this.bindingToNormalized;
    }

    public DOMSchemaService getSchemaService() {
        return this.schemaService;
    }

    public DOMDataBroker getDOMDataBroker() {
        if (this.domDataBroker == null) {
            this.domDataBroker = createDOMDataBroker();
        }
        return this.domDataBroker;
    }

    private synchronized ImmutableMap<LogicalDatastoreType, DOMStore> getDatastores() {
        if (this.datastores == null) {
            this.datastores = createDatastores();
        }
        return this.datastores;
    }

    public void updateSchema(final SchemaContext ctx) {
        this.schemaService.changeSchema(ctx);
    }

    public DOMNotificationRouter getDomNotificationRouter() {
        return this.domNotificationRouter;
    }
}
