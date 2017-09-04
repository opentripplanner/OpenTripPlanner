/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway2.gtfs.impl;

import org.onebusaway2.gtfs.model.IdentityBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

public class GenericDaoImpl {

    private final static Logger LOG = LoggerFactory.getLogger(GenericDaoImpl.class);

    private Map<Class<?>, Map<Object, Object>> entitiesByClassAndId = new HashMap<>();

    private Map<Class<?>, EntityHandler<Serializable>> handlers = new HashMap<Class<?>, EntityHandler<Serializable>>();

    public void clear() {
        entitiesByClassAndId.clear();
    }


    @SuppressWarnings("unchecked") <T> Collection<T> getAllEntitiesForType(Class<T> type) {
        Map<Object, Object> entitiesById = entitiesByClassAndId.get(type);
        if (entitiesById == null)
            return new ArrayList<T>();
        return (Collection<T>) entitiesById.values();
    }

    @SuppressWarnings("unchecked") <T> T getEntityForId(Class<T> type, Serializable id) {
        Map<Object, Object> byId = entitiesByClassAndId.get(type);

        if (byId == null) {
            LOG.debug("no stored entities type {}", type);
            return null;
        }

        return (T) byId.get(id);
    }

    @SuppressWarnings("unchecked") void saveEntity(Object entity) {

        Class<?> c = entity.getClass();

        EntityHandler<Serializable> handler = handlers.get(c);
        if (handler == null) {
            handler = (EntityHandler<Serializable>) createEntityHandler(c);
            handlers.put(c, handler);
        }

        IdentityBean<Serializable> bean = ((IdentityBean<Serializable>) entity);
        handler.handle(bean);

        Map<Object, Object> byId = entitiesByClassAndId.computeIfAbsent(c, k -> new HashMap<>());

        Object id = bean.getId();
        Object prev = byId.put(id, entity);
        if (prev != null)
            LOG.warn("entity with id already exists: class=" + c + " id=" + id + " prev=" + prev
                    + " new=" + entity);
    }


    /* Private Methods */

    private EntityHandler<?> createEntityHandler(Class<?> entityType) {
        try {
            Field field = entityType.getDeclaredField("id");
            if (field != null) {
                Class<?> type = field.getType();
                if (type.equals(Integer.class) || type.equals(Integer.TYPE))
                    return new GeneratedIdHandler();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return (EntityHandler<Serializable>) entity -> {
        };
    }

    private interface EntityHandler<T extends Serializable> {
        void handle(IdentityBean<T> entity);
    }

    private static class GeneratedIdHandler implements EntityHandler<Integer> {

        private int maxId = 0;

        public void handle(IdentityBean<Integer> entity) {
            Integer value = entity.getId();
            if (value == null || value == 0) {
                value = maxId + 1;
                entity.setId(value);
            }
            maxId = Math.max(maxId, value);
        }
    }

}
