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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Cheats;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingGridPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Reflection;

import java.util.List;

public class WndCheats extends Window {

	private static final int WIDTH = 120;
	private static final int BTN_HEIGHT = 16;
	private static final int TOGGLE_HEIGHT = 12;
	private static final int GAP = 2;

	private int pos;

	public WndCheats( Hero hero ) {
		pos = addTitle(this, Messages.get(WndGame.class, "cheats"), WIDTH);
		addRow( toggle(hero, "god_mode", Cheats.GodMode.class), toggle(hero, "one_hit_kill", Cheats.OneHitKill.class) );
		addRow( toggle(hero, "infinite_charges", Cheats.InfiniteCharges.class), toggle(hero, "no_cooldowns", Cheats.NoCooldowns.class) );
		addRow( toggle(hero, "infinite_durability", Cheats.InfiniteDurability.class), toggle(hero, "no_hunger", Cheats.NoHunger.class) );
		addSeparator();

		RedButton heal = new RedButton( Messages.get(this, "heal"), 6 ) {
			@Override
			protected void onClick() {
				Cheats.fullHeal(hero);
				text( Messages.get(WndCheats.class, "heal_done") );
				GLog.p( Messages.get(WndCheats.class, "healed") );
				Sample.INSTANCE.play( Assets.Sounds.DEWDROP );
			}
		};
		heal.icon(new ItemSprite(ItemSpriteSheet.DEWDROP));
		RedButton levelUp = new RedButton( Messages.get(this, hero.lvl < Hero.MAX_LEVEL ? "level_up" : "max_level"), 6 ) {
			@Override
			protected void onClick() {
				Cheats.levelUp(hero);
				text( hero.lvl < Hero.MAX_LEVEL ? Messages.get(WndCheats.class, "level", hero.lvl) : Messages.get(WndCheats.class, "max_level") );
				enable(hero.lvl < Hero.MAX_LEVEL);
				GLog.p( Messages.get(WndCheats.class, "level_reached", hero.lvl) );
			}
		};
		levelUp.icon(Icons.get(Icons.TALENT));
		levelUp.enable(hero.lvl < Hero.MAX_LEVEL);
		addRow( heal, levelUp );

		RedButton gold = new RedButton( Messages.get(this, "add_gold"), 6 ) {
			@Override
			protected void onClick() {
				Cheats.addGold();
				text( Messages.get(WndCheats.class, "gold_added") );
				GLog.p( Messages.get(WndCheats.class, "gold_log") );
				Sample.INSTANCE.play( Assets.Sounds.GOLD );
			}
		};
		gold.icon(new ItemSprite(ItemSpriteSheet.GOLD));
		RedButton identify = new RedButton( Messages.get(this, "identify_all"), 6 ) {
			@Override
			protected void onClick() {
				int count = Cheats.identifyInventory(hero);
				if (count > 0) {
					text( Messages.get(WndCheats.class, "identified", count) );
					GLog.i( Messages.get(WndCheats.class, "identified_log", count) );
					Sample.INSTANCE.play( Assets.Sounds.READ );
				} else {
					text( Messages.get(WndCheats.class, "all_known") );
					GLog.i( Messages.get(WndCheats.class, "nothing_to_identify") );
				}
			}
		};
		identify.icon(new ItemSprite(ItemSpriteSheet.STONE_INTUITION));
		addRow( gold, identify );
		addSeparator();

		RedButton revealMap = new RedButton( Messages.get(this, "reveal_map") ) {
			@Override
			protected void onClick() {
				Cheats.revealMap();
				WndCheats.this.hide();
				Sample.INSTANCE.play( Assets.Sounds.READ );
			}
		};
		revealMap.icon(new ItemSprite(ItemSpriteSheet.STONE_CLAIRVOYANCE));
		addRow( revealMap );

		RedButton warp = new RedButton( Messages.get(WndWarp.class, "title") ) {
			@Override
			protected void onClick() {
				if (Dungeon.interfloorTeleportAllowed()) {
					GameScene.show(new WndWarp());
				} else {
					GameScene.show(new WndMessage(Messages.get(ScrollOfTeleportation.class, "no_tele")));
				}
			}
		};
		warp.icon(new ItemSprite(ItemSpriteSheet.ARTIFACT_BEACON));
		addRow( warp );

		RedButton spawn = new RedButton( Messages.get(WndSpawn.class, "title") ) {
			@Override
			protected void onClick() {
				GameScene.show(new WndSpawn());
			}
		};
		spawn.icon(new ItemSprite(ItemSpriteSheet.CHEST));
		addRow( spawn );

		resize( WIDTH, pos - GAP );
	}

	private static int addTitle( Window window, String text, int width ) {
		RenderedTextBlock title = PixelScene.renderTextBlock(text, 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((width - title.width()) / 2f, 0);
		PixelScene.align(title);
		window.add(title);
		return (int) Math.ceil(title.bottom()) + GAP;
	}

	private void addRow( RedButton... buttons ) {
		boolean toggles = buttons[0] instanceof CheckBox;
		int buttonHeight = toggles ? TOGGLE_HEIGHT : BTN_HEIGHT;
		int buttonWidth = (WIDTH - (buttons.length - 1) * GAP) / buttons.length;
		for (int i = 0; i < buttons.length; i++) {
			if (!toggles && buttons.length > 1) buttons[i].multiline = true;
			add( buttons[i] );
			buttons[i].setRect( i * (buttonWidth + GAP), pos, buttonWidth, buttonHeight );
		}
		pos += buttonHeight + GAP;
	}

	private void addSeparator() {
		ColorBlock separator = new ColorBlock(WIDTH, 1, 0xFF000000);
		separator.y = pos;
		add(separator);
		pos += 1 + GAP;
	}

	private CheckBox toggle( Hero hero, String key, Class<? extends Buff> cheat ) {
		CheckBox button = new CheckBox( Messages.get(this, key) ) {
			@Override
			protected void onClick() {
				super.onClick();
				Cheats.setEnabled(hero, cheat, checked());
				GLog.i( Messages.get(WndCheats.class, key + (checked() ? "_on" : "_off")) );
			}
		};
		button.checked(hero.buff(cheat) != null);
		return button;
	}

	private static class WndWarp extends Window {

		private static final int FLOOR_HEIGHT = 13;

		WndWarp() {
			String[] regions = {"sewers", "prison", "caves", "city", "halls"};
			int top = addTitle(this, Messages.get(this, "title"), WIDTH);
			float buttonWidth = (WIDTH - 4 * GAP) / 5f;
			for (int region = 0; region < regions.length; region++) {
				RenderedTextBlock title = PixelScene.renderTextBlock(Document.INTROS.pageTitle(regions[region]), 6);
				title.setPos(0, top);
				add(title);
				top = (int) Math.ceil(title.bottom()) + 1;
				for (int i = 0; i < 5; i++) {
					int depth = region * 5 + i + 1;
					RedButton button = floorButton(depth, Integer.toString(depth));
					add(button);
					button.setRect(i * (buttonWidth + GAP), top, buttonWidth, FLOOR_HEIGHT);
				}
				top += FLOOR_HEIGHT + GAP;
			}

			RedButton lastFloor = floorButton(26, Messages.get(this, "floor", 26));
			lastFloor.icon(new ItemSprite(ItemSpriteSheet.AMULET));
			add(lastFloor);
			lastFloor.setRect(0, top, WIDTH, BTN_HEIGHT);
			resize(WIDTH, top + BTN_HEIGHT);
		}

		private RedButton floorButton( int depth, String label ) {
			RedButton button = new RedButton(label, 7) {
				@Override
				protected void onClick() {
					GLog.i( Messages.get(WndWarp.class, "warping", depth) );
					Cheats.warpToFloor(depth);
				}
			};
			if (depth == Dungeon.depth && Dungeon.branch == 0) {
				button.textColor(TITLE_COLOR);
				button.enable(false);
			} else if (depth % 5 == 0) {
				button.textColor(CharSprite.NEGATIVE);
			}
			return button;
		}
	}

	private static class WndSpawn extends Window {

		private final ScrollingGridPane grid = new ScrollingGridPane();
		private final RenderedTextBlock status = PixelScene.renderTextBlock(6);

		WndSpawn() {
			boolean landscape = PixelScene.landscape();
			resize( landscape ? WndJournal.WIDTH_L : WndJournal.WIDTH_P,
					landscape ? WndJournal.HEIGHT_L : WndJournal.HEIGHT_P );
			int top = addTitle(this, Messages.get(this, "title"), width);
			status.setPos(0, top);
			status.maxWidth(width);
			status.text(Messages.get(this, "prompt"));
			add(status);

			add(grid);
			addCatalogs(Catalog.equipmentCatalogs);
			addCatalogs(Catalog.consumableCatalogs);
			layoutGrid();
		}

		@Override
		public void offset( int xOffset, int yOffset ) {
			super.offset(xOffset, yOffset);
			layoutGrid();
		}

		private void layoutGrid() {
			int gridTop = (int) Math.ceil(status.bottom()) + GAP;
			grid.setRect(0, gridTop, width, height - gridTop);
			grid.scrollTo(0, grid.content().camera.scroll.y);
		}

		private void addCatalogs( List<Catalog> catalogs ) {
			for (Catalog catalog : catalogs) {
				boolean headerAdded = false;
				for (Class<?> itemClass : catalog.items()) {
					if (!Item.class.isAssignableFrom(itemClass)) continue;
					Item item = Reflection.newInstance(itemClass.asSubclass(Item.class));
					if (item instanceof Gold || item instanceof EnergyCrystal || item instanceof Dewdrop || item instanceof Key
							|| item instanceof Amulet || item instanceof EscapeCrystal || item instanceof CorpseDust) continue;
					if (!headerAdded) {
						grid.addHeader( Messages.titleCase(catalog.title()) );
						headerAdded = true;
					}
					addItem(item);
				}
			}
		}

		private void addItem( Item item ) {
			ScrollingGridPane.GridItem gridItem = new ScrollingGridPane.GridItem( new ItemSprite(item.image(), item.glowing()) ) {
				@Override
				public boolean onClick( float x, float y ) {
					if (!inside(x, y)) return false;
					Image previewIcon = new ItemSprite();
					previewIcon.copy(icon);
					showPreview(item.getClass(), previewIcon);
					return true;
				}
			};
			if (item.icon != -1) {
				Image icon = new Image( Assets.Sprites.ITEM_ICONS );
				icon.frame( ItemSpriteSheet.Icons.film.get(item.icon) );
				gridItem.addSecondIcon(icon);
			}
			grid.addItem(gridItem);
		}

		private void showPreview( Class<? extends Item> itemClass, Image icon ) {
			Item preview = Reflection.newInstance(itemClass);
			if (preview instanceof Ring) {
				((Ring) preview).anonymize();
			} else if (preview instanceof Potion) {
				((Potion) preview).anonymize();
			} else if (preview instanceof Scroll) {
				((Scroll) preview).anonymize();
			}

			String title = Messages.titleCase(preview.name());
			String description = preview instanceof ClassArmor || preview instanceof SpiritBow ? preview.desc() : preview.info();
			GameScene.show( new WndOptions(icon, title, description, Messages.get(this, "spawn")) {
				@Override
				protected void onSelect( int index ) {
					Item spawned = Reflection.newInstance(itemClass);
					boolean collected = Cheats.spawnItem(spawned);
					String message = collected ? Messages.get(WndSpawn.class, "added", spawned.name())
							: Messages.get(WndSpawn.class, "pack_full");
					status.text(message);
					layoutGrid();
					if (collected) {
						GLog.p(message);
						Sample.INSTANCE.play( Assets.Sounds.ITEM );
					} else {
						GLog.w(message);
					}
				}
			} );
		}
	}
}
