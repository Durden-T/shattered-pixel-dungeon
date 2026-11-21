/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Game;

public class Cheats {

	public static class Toggle extends Buff {
		{ revivePersists = true; }
	}

	public static class GodMode extends Toggle { }

	public static class OneHitKill extends Toggle { }

	public static class InfiniteCharges extends Toggle { }

	public static class NoCooldowns extends Toggle { }

	public static class InfiniteDurability extends Toggle { }

	public static class NoHunger extends Toggle { }

	public static void setEnabled( Hero hero, Class<? extends Buff> cheat, boolean enabled ) {
		if (enabled) {
			Buff.affect(hero, cheat);
			if (cheat == InfiniteCharges.class) refillCharges(hero);
			if (cheat == NoCooldowns.class) clearCooldowns(hero);
			if (cheat == NoHunger.class) Buff.affect(hero, Hunger.class).satisfy(Hunger.STARVING);
		} else {
			Buff.detach(hero, cheat);
		}
	}

	static void refreshTurn( Hero hero ) {
		if (hero.buff(InfiniteCharges.class) != null) refillCharges(hero);
		if (hero.buff(NoCooldowns.class) != null) clearCooldowns(hero);
	}

	private static void refillCharges( Hero hero ) {
		for (Wand.Charger charger : hero.buffs(Wand.Charger.class)) {
			charger.gainCharge(charger.wand().maxCharges);
		}
		for (Item item : hero.belongings) {
			if (item instanceof Artifact && !(item instanceof CapeOfThorns) && !(item instanceof HornOfPlenty)) {
				((Artifact) item).refillCharge();
			} else if (item instanceof ClassArmor && ((ClassArmor) item).charge < 100) {
				((ClassArmor) item).charge = 100;
				Item.updateQuickslot();
			}
		}
		if (hero.heroClass == HeroClass.DUELIST) {
			MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
			charger.gainCharge(charger.chargeCap() - charger.charges);
		}
		if (hero.subClass == HeroSubClass.MONK) {
			MonkEnergy monk = Buff.affect(hero, MonkEnergy.class);
			if (monk.energy < monk.energyCap()) {
				monk.energy = monk.energyCap();
				if (monk.cooldown == 0) ActionIndicator.setAction(monk);
				BuffIndicator.refreshHero();
			}
		}
	}

	private static void clearCooldowns( Hero hero ) {
		for (Buff buff : hero.buffs()) {
			if (buff instanceof Talent.AggressiveBarrierCooldown
					|| buff instanceof MonkEnergy.MonkAbility.FlurryCooldownTracker
					|| buff instanceof GuidingLight.GuidingLightPriestCooldown
					|| buff instanceof Talent.ImprovisedProjectileCooldown
					|| buff instanceof HolyLance.LanceCooldown
					|| buff instanceof Talent.LethalHasteCooldown
					|| buff instanceof Talent.RejuvenatingStepsCooldown
					|| buff instanceof Talent.SearingLightCooldown
					|| buff instanceof Talent.SeerShotCooldown
					|| buff instanceof Talent.SwiftEquipCooldown
					|| buff instanceof Shuriken.ShurikenInstantTracker
					|| (buff instanceof Talent.WandPreservationCounter && hero.pointsInTalent(Talent.WAND_PRESERVATION) == 2)) {
				buff.detach();
			} else if (buff instanceof Berserk) {
				((Berserk) buff).clearRecovery();
			} else if (buff instanceof Momentum) {
				((Momentum) buff).clearCooldown();
			} else if (buff instanceof BrokenSeal.WarriorShield) {
				((BrokenSeal.WarriorShield) buff).clearCooldown();
			} else if (buff instanceof MonkEnergy) {
				MonkEnergy monk = (MonkEnergy) buff;
				if (monk.cooldown > 0) {
					monk.cooldown = 0;
					if (monk.energy >= 1) ActionIndicator.setAction(monk);
					BuffIndicator.refreshHero();
				}
			}
		}
	}

	public static void fullHeal( Hero hero ) {
		PotionOfCleansing.cleanse(hero, 0);
		hero.HP = hero.HT;
	}

	public static void levelUp( Hero hero ) {
		if (hero.lvl < Hero.MAX_LEVEL) {
			hero.earnExp(hero.maxExp(), PotionOfExperience.class);
		}
	}

	public static void addGold() {
		Dungeon.gold += 1000;
		Item.updateQuickslot();
	}

	public static int identifyInventory( Hero hero ) {
		int unidentified = 0;
		for (Item item : hero.belongings) {
			if (!item.isIdentified()) unidentified++;
		}
		for (Item item : hero.belongings) {
			item.identify();
		}
		return unidentified;
	}

	public static void revealMap() {
		ScrollOfMagicMapping.revealMap();
	}

	public static void warpToFloor( int depth ) {
		Level.beforeTransition();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = depth;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = -1;
		Game.switchScene( InterlevelScene.class );
	}

	public static boolean spawnItem( Item item ) {
		if (!item.collect()) return false;
		if (item.quantity() == 0) item = Dungeon.hero.belongings.getSimilar(item);
		item.identify();
		return true;
	}
}
