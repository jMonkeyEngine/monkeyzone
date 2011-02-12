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

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.ai.commands.AttackCommand;
import com.jme3.monkeyzone.ai.Command;
import com.jme3.monkeyzone.ai.commands.FollowCommand;
import com.jme3.monkeyzone.ai.commands.MoveCommand;
import com.jme3.monkeyzone.ai.SphereTrigger;
import com.jme3.monkeyzone.controls.CommandControl;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the client input and UI for AI players
 * @author normenhansen
 */
public class ClientCommandInterface implements ActionListener {

    protected Screen screen;
    protected TextRenderer[] selectionTexts = new TextRenderer[10];
    protected TextRenderer[] commandTexts = new TextRenderer[10];
    protected List<Class<? extends Command>> commands = new LinkedList<Class<? extends Command>>();
    protected HashMap<Long, Spatial> players = new HashMap<Long, Spatial>();
    protected List<Spatial> selectedEntities = new ArrayList<Spatial>();
    protected InputManager inputManager;
    protected HashMap<Integer, LinkedList<Long>> playerGroups = new HashMap<Integer, LinkedList<Long>>();
    protected boolean shift = false;
    protected SelectionMenu currentSelectionMenu = SelectionMenu.Main;
    protected WorldManager world;

    protected enum SelectionMenu {

        Main,
        Offensive,
        Defensive,
        Builder,
        NavPoints
    }

    public ClientCommandInterface(Screen screen, InputManager inputManager) {
        this(inputManager);
        this.screen = screen;
        for (int i = 0; i < 10; i++) {
            selectionTexts[i] = screen.findElementByName("layer").findElementByName("panel_bottom").findElementByName("bottom_panel_right").findElementByName("status_text_0" + i).getRenderer(TextRenderer.class);
            commandTexts[i] = screen.findElementByName("layer").findElementByName("panel_top").findElementByName("top_panel_left").findElementByName("status_text_0" + i).getRenderer(TextRenderer.class);
        }
        setSelectionMenu(SelectionMenu.Main);
        updateCommandMenu();
    }

    public ClientCommandInterface(InputManager inputManager) {
        this.inputManager = inputManager;
        inputManager.addMapping("Client_Key_SHIFT", new KeyTrigger(KeyInput.KEY_RSHIFT), new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Client_Key_0", new KeyTrigger(KeyInput.KEY_0));
        inputManager.addMapping("Client_Key_1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Client_Key_2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Client_Key_3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("Client_Key_4", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("Client_Key_5", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping("Client_Key_6", new KeyTrigger(KeyInput.KEY_6));
        inputManager.addMapping("Client_Key_7", new KeyTrigger(KeyInput.KEY_7));
        inputManager.addMapping("Client_Key_8", new KeyTrigger(KeyInput.KEY_8));
        inputManager.addMapping("Client_Key_9", new KeyTrigger(KeyInput.KEY_9));
        inputManager.addMapping("Client_Key_F10", new KeyTrigger(KeyInput.KEY_F10));
        inputManager.addMapping("Client_Key_F1", new KeyTrigger(KeyInput.KEY_F1));
        inputManager.addMapping("Client_Key_F2", new KeyTrigger(KeyInput.KEY_F2));
        inputManager.addMapping("Client_Key_F3", new KeyTrigger(KeyInput.KEY_F3));
        inputManager.addMapping("Client_Key_F4", new KeyTrigger(KeyInput.KEY_F4));
        inputManager.addMapping("Client_Key_F5", new KeyTrigger(KeyInput.KEY_F5));
        inputManager.addMapping("Client_Key_F6", new KeyTrigger(KeyInput.KEY_F6));
        inputManager.addMapping("Client_Key_F7", new KeyTrigger(KeyInput.KEY_F7));
        inputManager.addMapping("Client_Key_F8", new KeyTrigger(KeyInput.KEY_F8));
        inputManager.addMapping("Client_Key_F9", new KeyTrigger(KeyInput.KEY_F9));
        inputManager.addListener(this,
                "Client_Key_SHIFT",
                "Client_Key_0",
                "Client_Key_1",
                "Client_Key_2",
                "Client_Key_3",
                "Client_Key_4",
                "Client_Key_5",
                "Client_Key_6",
                "Client_Key_7",
                "Client_Key_8",
                "Client_Key_9",
                "Client_Key_F10",
                "Client_Key_F1",
                "Client_Key_F2",
                "Client_Key_F3",
                "Client_Key_F4",
                "Client_Key_F5",
                "Client_Key_F6",
                "Client_Key_F7",
                "Client_Key_F8",
                "Client_Key_F9");
    }

    public void setWorldManager(WorldManager world) {
        this.world = world;
    }

    /**
     * adds a player id with entity to the list of user controlled entities,
     * called from WorldManager when a player that belongs to this user has
     * entered an entity.
     * @param id
     * @param entity
     */
    public void setPlayerEntity(long id, Spatial entity) {
        if (entity == null) {
            players.remove(id);
            return;
        }
        players.put(id, entity);
        //TODO: apply sphere command type via menu
        SphereTrigger sphereTrigger = entity.getControl(SphereTrigger.class);
        if (sphereTrigger != null) {
            sphereTrigger.setGhostRadius(20);
            //adding a command that will be used by the sphere trigger by default
            Command command = entity.getControl(CommandControl.class).initializeCommand(new AttackCommand());
            sphereTrigger.setCommand(command);
            setSelectionMenu(currentSelectionMenu);
        }
    }

    /**
     * clears the list of user controlled entities
     */
    public void clearPlayers() {
        players.clear();
        setSelectionMenu(currentSelectionMenu);
    }

    /**
     * removes a user controlled entity
     * @param id
     */
    public void removePlayerEntity(long id) {
        players.remove(id);
        setSelectionMenu(currentSelectionMenu);
    }

    /**
     * gets the command queue of a specific player
     * @param id
     * @return
     */
    public CommandControl getCommandQueue(long id) {
        if (!players.containsKey(id)) {
            return null;
        }
        return players.get(id).getControl(CommandControl.class);
    }

    /**
     * gets the SphereTrigger of a specific player
     * @param id
     * @return
     */
    public SphereTrigger getSphereTrigger(long id) {
        if (!players.containsKey(id)) {
            return null;
        }
        return players.get(id).getControl(SphereTrigger.class);
    }

    /**
     * clear the selection list and set entity as selected entity
     * @param entity
     */
    public void selectEntity(Spatial entity) {
        this.selectedEntities.clear();
        this.selectedEntities.add(entity);
        updateCommandMenu();
    }

    /**
     * set multiple entitis as the list of selected entities
     * @param entities
     */
    public void selectEntities(List<Spatial> entities) {
        this.selectedEntities.clear();
        this.selectedEntities.addAll(entities);
        updateCommandMenu();
    }

    /**
     * add a single entity to the list of selected entities
     * @param entity
     */
    public void addSelectEntity(Spatial entity) {
        this.selectedEntities.add(entity);
        updateCommandMenu();
    }

    /**
     * remove a single entity from the list of selected entities
     * @param entity
     */
    public void removeSelectEntity(Spatial entity) {
        this.selectedEntities.remove(entity);
        updateCommandMenu();
    }

    public void update(float tpf) {
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        int key = -1;
        int fkey = -1;
        if (name.equals("Client_Key_SHIFT")) {
            shift = isPressed;
        } else if (name.equals("Client_Key_0") && isPressed) {
            key = 0;
        } else if (name.equals("Client_Key_1") && isPressed) {
            key = 1;
        } else if (name.equals("Client_Key_2") && isPressed) {
            key = 2;
        } else if (name.equals("Client_Key_3") && isPressed) {
            key = 3;
        } else if (name.equals("Client_Key_4") && isPressed) {
            key = 4;
        } else if (name.equals("Client_Key_5") && isPressed) {
            key = 5;
        } else if (name.equals("Client_Key_6") && isPressed) {
            key = 6;
        } else if (name.equals("Client_Key_7") && isPressed) {
            key = 7;
        } else if (name.equals("Client_Key_8") && isPressed) {
            key = 8;
        } else if (name.equals("Client_Key_9") && isPressed) {
            key = 9;
        } else if (name.equals("Client_Key_F10") && isPressed) {
            fkey = 10;
        } else if (name.equals("Client_Key_F1") && isPressed) {
            fkey = 1;
        } else if (name.equals("Client_Key_F2") && isPressed) {
            fkey = 2;
        } else if (name.equals("Client_Key_F3") && isPressed) {
            fkey = 3;
        } else if (name.equals("Client_Key_F4") && isPressed) {
            fkey = 4;
        } else if (name.equals("Client_Key_F5") && isPressed) {
            fkey = 5;
        } else if (name.equals("Client_Key_F6") && isPressed) {
            fkey = 6;
        } else if (name.equals("Client_Key_F7") && isPressed) {
            fkey = 7;
        } else if (name.equals("Client_Key_F8") && isPressed) {
            fkey = 8;
        } else if (name.equals("Client_Key_F9") && isPressed) {
            fkey = 9;
        }
        if (key != -1) {
            processSelectionKey(key);
        } else if (fkey != -1) {
            processCommandKey(fkey);
        }
    }

    /**
     * displays a specific selection menu, compiles list of current entities
     * in that menu
     * @param key
     */
    private void setSelectionMenu(SelectionMenu key) {
        currentSelectionMenu = key;
        switch (currentSelectionMenu) {
            case Main:
                selectionTexts[1].changeText("1 - Offensive Units");
//                selectionTexts[2].changeText("2 - Defensive Units");
//                selectionTexts[3].changeText("3 - Builder Units");
//                selectionTexts[4].changeText("4 - Nav Points");
                selectionTexts[2].changeText("");
                selectionTexts[3].changeText("");
                selectionTexts[4].changeText("");
                selectionTexts[5].changeText("");
                selectionTexts[6].changeText("");
                selectionTexts[7].changeText("");
                selectionTexts[8].changeText("");
                selectionTexts[9].changeText("");
                selectionTexts[0].changeText("");
                break;
            case Offensive:
                clearSelectionMenu();
                int i = 0;
                for (Iterator<Entry<Long, Spatial>> it = players.entrySet().iterator(); it.hasNext();) {
                    i++;
                    Entry<Long, Spatial> entry = it.next();
                    if (i >= 10) {
                        i = 0;
                        if (selectedEntities.contains(entry.getValue())) {
                            selectionTexts[i].changeText(i + " - " + entry.getValue().getName() + " *");
                        } else {
                            selectionTexts[i].changeText(i + " - " + entry.getValue().getName());
                        }
                        return;
                    } else {
                        if (selectedEntities.contains(entry.getValue())) {
                            selectionTexts[i].changeText(i + " - " + entry.getValue().getName() + " *");
                        } else {
                            selectionTexts[i].changeText(i + " - " + entry.getValue().getName());
                        }
                    }
                }
                break;
        }
    }

    /**
     * clears the selection menu ui
     */
    private void clearSelectionMenu() {
        for (int i = 0; i < selectionTexts.length; i++) {
            TextRenderer textRenderer = selectionTexts[i];
            textRenderer.changeText("");
        }
    }

    /**
     * processes selection key (1-0) being pressed
     * @param key
     */
    private void processSelectionKey(int key) {
        switch (currentSelectionMenu) {
            case Main:
                switch (key) {
                    case 1:
                        setSelectionMenu(SelectionMenu.Offensive);
                        break;
//                    case 2:
//                        setSelectionMenu(SelectionMenu.Defensive);
//                        break;
//                    case 3:
//                        setSelectionMenu(SelectionMenu.Builder);
//                        break;
//                    case 4:
//                        setSelectionMenu(SelectionMenu.NavPoints);
//                        break;
                }
                break;
            case Offensive:
                selectUnit(currentSelectionMenu, key, shift);
                if (!shift) {
                    setSelectionMenu(SelectionMenu.Main);
                }
                break;
        }
    }

    /**
     * select a specific unit from the selection menu
     * @param key
     * @param add
     */
    private void selectUnit(SelectionMenu menu, int key, boolean add) {
        //TODO: filter for menu
        int i = 0;
        for (Iterator<Entry<Long, Spatial>> it = players.entrySet().iterator(); it.hasNext();) {
            i++;
            Entry<Long, Spatial> entry = it.next();
            if (i == key) {
                if (selectedEntities.contains(entry.getValue())) {
                    removeSelectEntity(entry.getValue());
                } else {
                    if (add) {
                        addSelectEntity(entry.getValue());
                    } else {
                        selectEntity(entry.getValue());
                    }
                }
            }
        }
        //update menu
        setSelectionMenu(currentSelectionMenu);
    }

    /**
     * updates the command menu based on the currently selected entities
     */
    private void updateCommandMenu() {
        commands.clear();
        if (selectedEntities.size() > 0) {
            commands.add(MoveCommand.class);
            commands.add(FollowCommand.class);
            commands.add(AttackCommand.class);
        }
        clearCommandMenu();
        int i = 0;
        for (Iterator<Class<? extends Command>> it = commands.iterator(); it.hasNext();) {
            i++;
            try {
                Class<? extends Command> class1 = it.next();
                commandTexts[i].changeText("F" + i + " - " + class1.newInstance().getName() + "  ");
            } catch (InstantiationException ex) {
                Logger.getLogger(ClientCommandInterface.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ClientCommandInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (i > 10) {
                return;
            }
        }
    }

    /**
     * clears the command menu UI
     */
    private void clearCommandMenu() {
        for (int i = 0; i < commandTexts.length; i++) {
            TextRenderer textRenderer = commandTexts[i];
            textRenderer.changeText("");
        }
    }

    /**
     * processes a command key
     * @param key
     */
    private void processCommandKey(int key) {
        Long entityId = PlayerData.getLongData(world.getMyPlayerId(), "entity_id");
        if (entityId != -1) {
            doCommand(key - 1, world.getEntity(entityId));
        }
    }

    /**
     * issues a command with a spatial target to the selected entities
     * @param command
     * @param spatial
     */
    private void doCommand(int command, Spatial spatial) {
        for (Iterator<Spatial> it = selectedEntities.iterator(); it.hasNext();) {
            Spatial spatial1 = it.next();
            CommandControl commandControl = spatial1.getControl(CommandControl.class);
            if (commandControl == null) {
                Logger.getLogger(ClientCommandInterface.class.getName()).log(Level.WARNING, "Cannot apply command");
                continue;
            }
            try {
                Command commandInst = commands.get(command).newInstance();
                commandInst.setPriority(10);
                commandControl.clearCommands();
                commandControl.initializeCommand(commandInst);
                if (commandInst.setTargetEntity(spatial)) {
                    commandControl.addCommand(commandInst);
                }
            } catch (InstantiationException ex) {
                Logger.getLogger(ClientCommandInterface.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ClientCommandInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * issues a command with a location target to the selected entities
     * @param command
     * @param location
     */
    private void doCommand(int command, Vector3f location) {
        for (Iterator<Spatial> it = selectedEntities.iterator(); it.hasNext();) {
            Spatial spatial1 = it.next();
            CommandControl commandControl = spatial1.getControl(CommandControl.class);
            if (commandControl == null) {
                return;
            }
            try {
                Command commandInst = commands.get(command).newInstance();
                commandInst.setPriority(10);
                commandControl.clearCommands();
                commandControl.initializeCommand(commandInst);
                if (commandInst.setTargetLocation(location)) {
                    commandControl.addCommand(commandInst);
                }
            } catch (InstantiationException ex) {
                Logger.getLogger(ClientCommandInterface.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ClientCommandInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
