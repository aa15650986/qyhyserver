package com.zhuoan.biz.event.circle;

import com.corundumstudio.socketio.SocketIOClient;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.UUID;

public class ClientObservable extends Observable {
    private static volatile Hashtable<UUID, Hashtable<String, Observer>> uuidObserverMap = new Hashtable();
    private static Hashtable<UUID, SocketIOClient> uuidClientMap = new Hashtable();
    private String observerKey = "";

    public ClientObservable(String observerKey) {
        this.observerKey = observerKey;
    }

    public void notifyObservers(Object key) {
        super.setChanged();
        super.notifyObservers(key);
    }

    public void notifyObservers() {
        super.setChanged();
        super.notifyObservers();
    }

    private void clearDeadConnections() {
        if ((new Random()).nextInt(100) == 0) {
            Class var1 = ClientObservable.class;
            synchronized(ClientObservable.class) {
                Iterator itr = uuidClientMap.keySet().iterator();

                while(true) {
                    UUID uuid;
                    SocketIOClient client;
                    do {
                        if (!itr.hasNext()) {
                            return;
                        }

                        uuid = (UUID)itr.next();
                        client = (SocketIOClient)uuidClientMap.get(uuid);
                    } while(client.isChannelOpen());

                    Hashtable<String, Observer> obsMap = (Hashtable)uuidObserverMap.get(uuid);
                    if (obsMap != null) {
                        Iterator var6 = obsMap.values().iterator();

                        while(var6.hasNext()) {
                            Observer obs = (Observer)var6.next();
                            this.deleteObserver(obs);
                        }
                    }

                    itr.remove();
                    uuidObserverMap.remove(uuid);
                }
            }
        }
    }

    public boolean contains(SocketIOClient client) {
        return uuidObserverMap.containsKey(client.getSessionId());
    }

    public void deleteObserverByClient(SocketIOClient client) {
        Hashtable<String, Observer> obsMap = (Hashtable)uuidObserverMap.get(client.getSessionId());
        if (obsMap != null) {
            Observer obs = (Observer)obsMap.get(this.observerKey);
            this.deleteObserver(obs);
            obsMap.remove(this.observerKey);
        }

    }

    public void addObserverByClient(SocketIOClient client, Observer observer) {
        this.clearDeadConnections();
        this.addObserver(observer);
        Hashtable<String, Observer> obsMap = (Hashtable)uuidObserverMap.get(client.getSessionId());
        if (obsMap == null) {
            synchronized(this) {
                if (obsMap == null) {
                    obsMap = new Hashtable();
                    uuidObserverMap.put(client.getSessionId(), obsMap);
                }
            }
        }

        obsMap.put(this.observerKey, observer);
        uuidClientMap.put(client.getSessionId(), client);
    }
}
