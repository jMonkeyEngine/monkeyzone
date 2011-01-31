/*
 * Copyright (c) 2009-2011 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.monkeyzone;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Basic class to store data about players (Human and AI), could be replaced by
 * a database or similar, static access with synchronization. Access is assured
 * to be sequential during the game, so in theory syncing is not needed. Used on
 * server and on client.
 * @author normenhansen
 */
public class PlayerData {

    private static HashMap<Long, PlayerData> players = new HashMap<Long, PlayerData>();
    private long id;
    private int aiControl = -1;
    private HashMap<String, Float> floatData = new HashMap<String, Float>();
    private HashMap<String, Integer> intData = new HashMap<String, Integer>();
    private HashMap<String, Long> longData = new HashMap<String, Long>();
    private HashMap<String, Boolean> booleanData = new HashMap<String, Boolean>();
    private HashMap<String, String> stringData = new HashMap<String, String>();

    public static synchronized List<PlayerData> getHumanPlayers() {
        LinkedList<PlayerData> list = new LinkedList<PlayerData>();
        for (Iterator<Entry<Long, PlayerData>> it = players.entrySet().iterator(); it.hasNext();) {
            Entry<Long, PlayerData> entry = it.next();
            if (entry.getValue().isHuman()) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    public static synchronized List<PlayerData> getPlayers() {
        LinkedList<PlayerData> list = new LinkedList<PlayerData>(players.values());
        return list;
    }

    public static synchronized long getNew(String name) {
        long id = 0;
        while (players.containsKey(id)) {
            id++;
        }
        players.put(id, new PlayerData(id, name));
        return id;
    }

    public static synchronized void add(long id, PlayerData player) {
        players.put(id, player);
    }

    public static synchronized void remove(long id) {
        players.remove(id);
    }

    public static synchronized int getAiControl(long id) {
        return players.get(id).getAiControl();
    }

    public static synchronized void setAiControl(long id, int aiControl) {
        players.get(id).setAiControl(aiControl);
    }

    public static synchronized boolean isHuman(long id) {
        return players.get(id).isHuman();
    }

    public static synchronized float getFloatData(long id, String key) {
        if (!players.containsKey(id)) return -1;
        return players.get(id).getFloatData(key);
    }

    public static synchronized void setData(long id, String key, float data) {
        if (!players.containsKey(id)) return;
        players.get(id).setData(key, data);
    }

    public static synchronized int getIntData(long id, String key) {
        if (!players.containsKey(id)) return -1;
        return players.get(id).getIntData(key);
    }

    public static synchronized void setData(long id, String key, int data) {
        if (!players.containsKey(id)) return;
        players.get(id).setData(key, data);
    }

    public static synchronized long getLongData(long id, String key) {
        if (!players.containsKey(id)) return -1;
        return players.get(id).getLongData(key);
    }

    public static synchronized void setData(long id, String key, long data) {
        if (!players.containsKey(id)) return;
        players.get(id).setData(key, data);
    }

    public static synchronized boolean getBooleanData(long id, String key) {
        if (!players.containsKey(id)) return false;
        return players.get(id).getBooleanData(key);
    }

    public static synchronized void setData(long id, String key, boolean data) {
        if (!players.containsKey(id)) return;
        players.get(id).setData(key, data);
    }

    public static synchronized String getStringData(long id, String key) {
        if (!players.containsKey(id)) return "unknown";
        return players.get(id).getStringData(key);
    }

    public static synchronized void setData(long id, String key, String data) {
        if (!players.containsKey(id)) return;
        players.get(id).setData(key, data);
    }

    public PlayerData(long id) {
        this.id = id;
    }

    /**
     * Object implementation of PlayerData
     */
    public PlayerData(long id, int groupId, String name) {
        this(id, groupId, name, -1);
    }

    /**
     * Object implementation of PlayerData
     */
    public PlayerData(long id, String name) {
        this.id = id;
        setData("name", name);
        setData("entity_id", (long) -1);
    }

    public PlayerData(long id, int groupId, String name, int aiControl) {
        this.id = id;
        this.aiControl = aiControl;
        setData("group_id", groupId);
        setData("name", name);
        setData("entity_id", (long) -1);
    }

    public long getId() {
        return id;
    }

    public int getAiControl() {
        return aiControl;
    }

    public void setAiControl(int aiControl) {
        this.aiControl = aiControl;
    }

    public boolean isHuman() {
        return aiControl == -1;
    }

    public float getFloatData(String key) {
        return floatData.get(key);
    }

    public void setData(String key, float data) {
        floatData.put(key, data);
    }

    public int getIntData(String key) {
        return intData.get(key);
    }

    public void setData(String key, int data) {
        intData.put(key, data);
    }

    public long getLongData(String key) {
        return longData.get(key);
    }

    public void setData(String key, long data) {
        longData.put(key, data);
    }

    public boolean getBooleanData(String key) {
        return booleanData.get(key);
    }

    public void setData(String key, boolean data) {
        booleanData.put(key, data);
    }

    public String getStringData(String key) {
        return stringData.get(key);
    }

    public void setData(String key, String data) {
        stringData.put(key, data);
    }
}
