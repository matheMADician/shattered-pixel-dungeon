/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.quickslot;

public class Torch extends Item {

	public static final String AC_LIGHT	= "LIGHT";

	public static final String AC_EXTINGUISH = "EXTINGUISH"; //mod:
	
	public static final float TIME_TO_LIGHT = 3; //mod: punish

	public static float DURABILITY = 200;

	private boolean IS_LIT = false;
	{
		image = ItemSpriteSheet.TORCH;
		
		stackable = false; //mod: enable DURABILITY counting
		
		//defaultAction = AC_LIGHT; //mod:
	}

	//mod: vvv-------------------------------------------------------------------------------------------------------vvv
	@Override
	public String defaultAction(){
		return IS_LIT ? AC_EXTINGUISH : AC_LIGHT;
	}

	@Override
	public String info() {
		String info = desc();
		info += "\n";
		info += Messages.get(Torch.class , "durability" , DURABILITY);
		return info;
	}

	//mod: ^^^-------------------------------------------------------------------------------------------------------^^^

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_LIGHT );
		actions.add( AC_EXTINGUISH );
		return actions;
	}
	
	@Override
	public boolean execute( Hero hero, String action ) {

		super.execute( hero, action );

		if(! super.execute( hero, action )){ //mod: if the hero didn't drop, throw, or if the item is not in hotbar
			GLog.i(Messages.get(QuickSlot.class , "warning"));
		}else {

			if (action.equals(AC_LIGHT) && IS_LIT == false) { //mod:

				this.IS_LIT = true; //mod:

				hero.spend(TIME_TO_LIGHT);
				hero.busy();

				hero.sprite.operate(hero.pos);

				//detach( hero.belongings.backpack ); //mod:

				Buff.affect(hero, Light.class, DURABILITY); //mod:
				Sample.INSTANCE.play(Assets.Sounds.BURNING);

				Emitter emitter = hero.sprite.centerEmitter();
				emitter.start(FlameParticle.FACTORY, 0.2f, 3);

			} else if (action.equals(AC_EXTINGUISH) && this.IS_LIT) { //mod: adds extinguishing vvv-------------------------vvv

				this.IS_LIT = false;

				hero.spend(TIME_TO_LIGHT / 3);
				hero.busy();

				hero.sprite.operate(hero.pos);

				if (hero.buff(Light.class) == null) { //mod: if no illumination and lit, the durability must've run out
					this.detach(hero.belongings.backpack);
				} else {
					DURABILITY = hero.buff(Light.class).visualcooldown();
					hero.buff(Light.class).detach();
				}

			}//mod:^^^---------------------------------------------------------------------------------------------------^^^
		}
		return true;
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	@Override
	public int value() {
		return 8 * quantity;
	}

}
