package org.academiadecodigo.codecadets;

import org.academiadecodigo.codecadets.Configs.GameConfigs;
import org.academiadecodigo.codecadets.enums.GameStates;
import org.academiadecodigo.codecadets.enums.SoundTypes;
import org.academiadecodigo.codecadets.exceptions.UnknownTargetException;
import org.academiadecodigo.codecadets.exceptions.UnknownWeaponException;
import org.academiadecodigo.codecadets.enums.TargetSide;
import org.academiadecodigo.codecadets.gameobjects.Target;
import org.academiadecodigo.codecadets.gameobjects.enemies.Enemy;
import org.academiadecodigo.codecadets.gameobjects.props.Prop;
import org.academiadecodigo.codecadets.gameobjects.weapons.Weapon;
import org.academiadecodigo.codecadets.handlers.DuckKeyboardHandler;
import org.academiadecodigo.codecadets.handlers.DuckMouseHandler;
import org.academiadecodigo.codecadets.renderer.Renderer;
import org.academiadecodigo.codecadets.sound.Sound;
import org.academiadecodigo.simplegraphics.graphics.Rectangle;
import org.academiadecodigo.simplegraphics.graphics.Text;
import org.academiadecodigo.simplegraphics.pictures.Picture;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Game {

    private Renderer renderer;
    private Player player;
    private DuckMouseHandler mouseHandler;
    private DuckKeyboardHandler keyboardHandler;
    private Sound soundEngine;
    private StartPage startPage;

    // Game Properties
    private boolean gameEnded;
    private GameStates gameState;
    private Set<Target> targetHashList;

    private boolean restartGame;
    private boolean forceRestart;
    private boolean handlersCreated;

    private int targetsNumber;

    public Game() {
        this.restartGame = true;
        this.handlersCreated = false;
        this.soundEngine = new Sound();


    }

    public void showStartMenu() {
        this.gameState = GameStates.GAMESTARTING;
        this.keyboardHandler = new DuckKeyboardHandler(this);
        keyboardHandler.activateControls();
        startPage = new StartPage();
        soundEngine.playSound(SoundTypes.GAMENAME);
        startPage.init();
        this.soundEngine.playSound(SoundTypes.BGMUSIC);
    }

    public void init(String player) {

        if (renderer != null) {

            renderer.deleteAll();
        }

        this.player = new Player(player);
        this.player.init();
        this.renderer = new Renderer();
        this.renderer.initRender();
        this.targetHashList = new HashSet<>();
        this.mouseHandler = new DuckMouseHandler(this, this.renderer);
        mouseHandler.initMouse();

        if (!handlersCreated) {

            mouseHandler.initMouseClick();
            handlersCreated = true;
        }
    }

    public void gameStart() {

        try {

            player.changeWeapon(WeaponsFactory.createWeapon());

        } catch (UnknownWeaponException ex) {

            System.out.println("Error Creating Weapon on Game");
        }

        player.getScore().resetScore();
        renderer.drawClips(player.getWeapon().getType().getClips());
        renderer.reloadAmmo(player.getWeapon().getType().getClipBullets());
        renderer.drawWeapon(player.getWeapon());
        renderer.drawScore(player.getScore().getScore());
        this.gameState = GameStates.GAMEPLAYING;
        this.gameEnded = false;
        this.forceRestart = false;

        while (targetsNumber < 5 ) {
            targetsNumber = (int) (Math.random() * GameConfigs.MAX_TARGETS_NUMBER) ;
        }

        while (!gameEnded) {

            try {

                Thread.sleep(GameConfigs.GAME_SLEEP_TIME);

            } catch (InterruptedException ex) {

                System.out.println("Game Loop Exception: " + ex.getMessage());
            }

            tick();
        }

        switch (gameState) {

            case GAMEENDEDNOAMMO:
            case GAMEENDED:

                Text endGameTxt = renderer.newText(250, 200, 150, 30, "Game Over! Press X To Exit! Press R to restart!");

                if (gameState == GameStates.GAMEENDEDNOAMMO) {

                    Text endGameTxtNoAmmo = renderer.newText(300, 120, 60, 20, "No More Ammo");
                }

                gameEnded();

                break;

            default:
                System.out.println("WTF game state is that?: " + gameState.name());
        }
    }

    private void gameEnded() {

        while (gameEnded && restartGame) {

            try {

                Thread.sleep(GameConfigs.GAME_SLEEP_TIME);

            } catch (InterruptedException ex) {

                System.out.println("Game Loop Exception: " + ex.getMessage());
            }
        }
    }

    private void tick() {
        // Check if no more ammo
        if (player.getWeapon().getAmmo() == 0 &&
                player.getWeapon().getClips() == 0) {
            this.gameEnded = true;
            this.gameState = GameStates.GAMEENDEDNOAMMO;
        }

        //Add random target
        if (Math.random() < 0.20 && targetHashList.size() < targetsNumber) {
            try {

                if (Math.random() < 0.80) {
                    targetHashList.add(TargetsFactory.createEnemy());
                } else {
                    targetHashList.add(TargetsFactory.createProps());
                }

            } catch (UnknownTargetException ex) {
                System.out.println("Error Creating Target!");
            }
        }

        //Change every target Position && Remove if out of window
        Iterator<Target> iterator = targetHashList.iterator();
        while (iterator.hasNext()) {

            Target myTarget = iterator.next();

            if ((myTarget.getTargetSide() == TargetSide.LEFT &&
                    myTarget.getPicture().getX() >= renderer.getCanvas().getWidth() -
                            myTarget.getPicture().getWidth() - myTarget.getSpeedX()) ||
                    (myTarget.getTargetSide() == TargetSide.RIGHT &&
                            myTarget.getPicture().getX() <= myTarget.getSpeedX())) {

                if (Math.random() > 0.4) {

                    iterator.remove();
                    myTarget.getPicture().delete();

                } else {

                    switch (myTarget.getTargetSide()) {

                        case LEFT:
                            myTarget.setTargetSide(TargetSide.RIGHT);
                            myTarget.getPicture().delete();
                            myTarget.setPicture(myTarget.getRightPicture());
                            myTarget.getPicture().draw();
                            myTarget.setSpeedX(-myTarget.getSpeedX());
                            break;

                        case RIGHT:
                            myTarget.setTargetSide(TargetSide.LEFT);
                            myTarget.getPicture().delete();
                            myTarget.setPicture(myTarget.getLeftPicture());
                            myTarget.getPicture().draw();
                            myTarget.setSpeedX(-myTarget.getSpeedX());
                    }
                }
            }

            try {

                myTarget.move();

            } catch (ConcurrentModificationException ex) {

                System.out.println("Faulty Frame!\n");
            }


            //Check if force Restarted
            if (forceRestart) {

                gameEnded = true;
                gameState = GameStates.GAMEENDED;
            }
        }
    }

    public void eventShoot() {
        Weapon weapon = player.getWeapon();
        boolean killedOne = false;
        boolean hitTarget = false;

        if (weapon.getAmmo() == 0) {
            soundEngine.playSound(SoundTypes.SGEMPTY);
            return;
        }
        soundEngine.playSound(SoundTypes.SGSHOOT);


        Iterator<Target> iterator = targetHashList.iterator();

        while (iterator.hasNext() && !killedOne && !hitTarget) {

            Target target = iterator.next();

            if (target == null || target.getPosition() == null) {

                continue;
            }

            if (weapon.getAim().getX() < target.getPosition().getX() - weapon.getType().getSpread()) {

                continue;
            }

            if (weapon.getAim().getX() > target.getPosition().getX() + target.getPicture().getWidth() + weapon.getType().getSpread()) {

                continue;
            }

            if (weapon.getAim().getY() < target.getPosition().getY() - weapon.getType().getSpread()) {

                continue;
            }

            if (weapon.getAim().getY() > target.getPosition().getY() + target.getPicture().getHeight() + weapon.getType().getSpread()) {

                continue;
            }

            if (target instanceof Enemy) {

                Enemy ourEnemy = (Enemy) target;
                int enemyScore = ourEnemy.getType().getScore();

                if (getPlayer().getWeapon().getAmmo() > 0) {


                    if (Math.random() < 0.7) {

                        soundEngine.playSound(SoundTypes.DUCKHIT);

                    } else {

                        soundEngine.playSound(SoundTypes.DUCKHIT2);
                    }

                    boolean enemyKilled = weapon.shoot(target);

                    if (enemyKilled) {

                        player.getScore().changeScore(enemyScore);
                        target.getPicture().delete();
                        iterator.remove();
                        killedOne = true;
                        renderer.drawScore(player.getScore().getScore());
                    }
                    renderer.drawAmmo(player.getWeapon().getAmmo(), player.getWeapon().getType().getClipBullets());
                    renderer.drawClips(player.getWeapon().getClips());
                }
                hitTarget = true;
            }

            if (target instanceof Prop) {

                Prop ourProp = (Prop) target;

                if (getPlayer().getWeapon().getAmmo() > 0) {
                    soundEngine.playSound(SoundTypes.PROPHIT);

                    weapon.shoot(ourProp);
                    ourProp.getPowerup().activate(this);
                    target.getPicture().delete();
                    iterator.remove();

                    renderer.drawAmmo(player.getWeapon().getAmmo(), player.getWeapon().getType().getClipBullets());
                    renderer.drawClips(player.getWeapon().getClips());

                }


                hitTarget = true;
            }
        }

        if (killedOne || hitTarget) {

            return;
        }

        weapon.shoot(null);
        renderer.drawAmmo(player.getWeapon().getAmmo(), player.getWeapon().getType().getClipBullets());
    }

    public void reloadWeapon() {

        if (getPlayer().getWeapon().getClips() == 0) {
            return;
        }

        soundEngine.playSound(SoundTypes.SGRELOADING);
        this.player.getWeapon().reload();
        renderer.reloadAmmo(player.getWeapon().getType().getClipBullets());
        renderer.drawClips(player.getWeapon().getClips());
    }

    public void eventRestart() {

        this.gameEnded = false;
        this.restartGame = true;
    }

    public void updateCursor(Position event) {
        //Canvas
        Rectangle canvas = renderer.getCanvas();

        //Crosshair
        Picture crosshair = renderer.getCrosshair();

        //Get crossairHalfSizes
        int crosshairHalfWidth = (crosshair.getWidth() / 2);
        int crosshairHalfHeight = (crosshair.getHeight() / 2);


        //Get Player Weapon Aim
        Position weaponAim = getPlayer().getWeapon().getAim();

        //Set Player Aim Position
        weaponAim.setX(event.getX() - 11);
        weaponAim.setY(event.getY() - 32);

        Position aimPos = new Position(weaponAim.getX() - crosshairHalfWidth, weaponAim.getY() - crosshairHalfHeight);


        //Check if Crosshair not out of bounds of our window
        if (event.getX() >= canvas.getWidth() - (crosshairHalfWidth - 10)) {
            aimPos.setX(canvas.getWidth() - (crosshair.getWidth()));
        }

        if (event.getY() >= canvas.getHeight() - (crosshairHalfHeight - 30)) {
            aimPos.setY(canvas.getHeight() - (crosshair.getHeight()));
        }

        renderer.drawAim(aimPos);
    }

    public Player getPlayer() {
        return this.player;
    }

    public GameStates getGameState() {
        return this.gameState;
    }

    public void setForceRestart(boolean forceRestart) {
        this.forceRestart = forceRestart;
    }

    public boolean getRestartGame() {
        return restartGame;
    }

    public void setRestartGame(boolean restartGame) {
        this.restartGame = restartGame;
    }

    public StartPage getStartPage() {
        return startPage;
    }

    public Sound getSoundEngine() {
        return soundEngine;
    }
}