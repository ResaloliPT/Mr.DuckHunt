package org.academiadecodigo.codecadets;

import org.academiadecodigo.codecadets.enums.WeaponTypes;
import org.academiadecodigo.codecadets.gameobjects.weapons.Weapon;

public class FactoryWeapons {

    public static Weapon createWeapon() {
       int random = (int)(Math.random() * WeaponTypes.values().length);
       WeaponTypes weaponType = WeaponTypes.values()[random];

       Weapon weapon = null;

       switch (weaponType){
           case SHOTGUN:
               weapon = new Shotgun();
               break;

           default:
               weapon = new Shotgun();
       }

        return weapon;
    }
}
