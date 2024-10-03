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

package com.watabou.noosa;

import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.utils.Rect;
import com.watabou.utils.RectF;


import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

//import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
//import com.shatteredpixel.shatteredpixeldungeon.levels.painters.RegularPainter;

public class Tilemap extends Visual{

	protected SmartTexture texture;
	protected TextureFilm tileset;

	protected int[] data;
	protected int mapWidth;
	protected int mapHeight;
	protected int size;

	private float cellW;
	private float cellH;

	protected float[] vertices;
	protected FloatBuffer quads;
	protected Vertexbuffer buffer;

	private volatile Rect updated;
	private boolean fullUpdate;
	private Rect updating;
	private int topLeftUpdating;
	private int bottomRightUpdating;

	//mod: rendering chunk-------------------------------------------------------------------------------------------vvv
	public Rect LastUpdating;
	public int pos;
	public static int HeroX;
	public static int HeroY;

	protected static int DungeonMapSizeX;
	protected static int DungeonMapSizeY;
	private int TileX;
	private int TileY;
	protected static final int ChunkSize = 8; //mod: rendering chunksize

	protected static int ChunkNumX;
	protected static int ChunkNumY;

	public static void UpdateMapSize( int x, int y){
		DungeonMapSizeX = x;
		DungeonMapSizeY = y;
		ChunkNumX = x / ChunkSize;
		ChunkNumY = y / ChunkSize;
	}
	public static int LoadLeft(){
		return HeroX / ChunkSize == 0 ? 0 : (HeroX / ChunkSize - 1) * ChunkSize;
	}
	public static int LoadUp(){
		return HeroY / ChunkSize == 0 ? 0 : (HeroY / ChunkSize - 1) * ChunkSize;
	}
	public static int LoadWidth(){
		return (HeroX / ChunkSize == ChunkNumX - 1 || HeroX / ChunkSize == 0? DungeonMapSizeX: (HeroX / ChunkSize + 1) * ChunkSize) - LoadLeft();
	}
	public static int LoadHeight(){
		return (HeroY / ChunkSize == ChunkNumY - 1 || HeroY / ChunkSize == 0 ? DungeonMapSizeY: (HeroY / ChunkSize + 1) * ChunkSize) - LoadUp();
	}

	public static void UpdateHeroPos( int x, int y ){
		HeroX = x;
		HeroY = y;
	}
	//mod: rendering chunk-------------------------------------------------------------------------------------------^^^

	public Tilemap( Object tx, TextureFilm tileset ) {

		super( 0, 0, 0, 0 );

		this.texture = TextureCache.get( tx );
		this.tileset = tileset;

		RectF r = tileset.get( 0 );
		cellW = tileset.width( r );
		cellH = tileset.height( r );

		vertices = new float[16];

		updated = new Rect();


		//y = hero.pos / RegularPainter.ModMapSize;


	}

	public void map( int[] data, int cols ) {

		this.data = data;

		mapWidth = cols;
		mapHeight = data.length / cols;


		size = LoadWidth() * LoadHeight();

		width = cellW * LoadWidth();
		height = cellH * LoadHeight();

		quads = Quad.createSet( size ); //mod: rendering the size of quads

		updateMap();
	}
	
	public Image image(int x, int y){
		if (!needsRender(x + mapWidth*y)){
			return null;
		}else{
			Image img = new Image(texture);
			img.frame(tileset.get(data[x + mapWidth * y]));
			return img;
		}
	}

	//forces a full update, including new buffer
	public synchronized void updateMap( ){
		//updated.set( 0, 0, mapWidth, mapHeight ); //mod: change update
		updated.set( LoadLeft(), LoadUp(), LoadLeft() + LoadWidth(), LoadUp() + LoadHeight() );
		fullUpdate = true; //mod: no use to change?
	}

	public synchronized void updateMapCell(int cell){
		//updated.union( cell % mapWidth, cell / mapWidth ); //mod: rendering Removed because this contradict with
		//chunk rendering
	}

	private synchronized void moveToUpdating(){
		updating = new Rect(updated);
		updated.setEmpty();
	}

	protected void updateVertices() {

		moveToUpdating();
		
		float x1, y1, x2, y2;
		//int pos;
		RectF uv;

		y1 = cellH * updating.top;
		y2 = y1 + cellH;

		for (int i=updating.top; i < updating.bottom; i++) {

			x1 = cellW * updating.left;
			x2 = x1 + cellW;

			pos = i * mapWidth + updating.left;

			for (int j=updating.left; j < updating.right; j++) {

				//mod: ----------------------------------------------------------------------------------------------vvv
				//mod: rendering there are two coordinate systems, position on the map and position on the updating
				// rect, this used to not be the case because in SPD, the two coordinate systems coincide
				int bufferPos = ((pos % mapWidth - LoadLeft()) + (pos / mapWidth - LoadUp()) * LoadWidth());

				if (topLeftUpdating == -1)
					//topLeftUpdating = pos;
					topLeftUpdating = bufferPos;

				//bottomRightUpdating = pos + 1;
				bottomRightUpdating = bufferPos + 1;

				//((Buffer)quads).position(pos*16);

				if (bufferPos < 0 || bufferPos * 16 >= quads.capacity()) {
					throw new RuntimeException("Buffer position out of bounds: bufferPos:" + bufferPos + "\npos:" + pos +
							"\nposx:" + pos % mapWidth + "\nUpdatingTop:" + updating.top + "\n UpdatingLeft:" + updating.left +
							"\nposy:" + pos / mapWidth + "\nHeroX:" + HeroX + "\nHeroY:" + HeroY +
							"\nLoadLeft:" + LoadLeft() + "\nLoadUp:" + LoadUp() + "\nLoadWidth:" + LoadWidth() + "\nLoadHeight:"
							+ LoadHeight() + "\nmapWidth:" + mapWidth + "\nmapHeight:" + mapHeight + "\ni" + i + "\nj" + j);
				}
				((Buffer)quads).position(bufferPos * 16);
				//mod: ----------------------------------------------------------------------------------------------^^^
				
				uv = tileset.get(data[pos]);
				
				if (needsRender(pos) && uv != null) {

					vertices[0] = x1;
					vertices[1] = y1;

					vertices[2] = uv.left;
					vertices[3] = uv.top;

					vertices[4] = x2;
					vertices[5] = y1;

					vertices[6] = uv.right;
					vertices[7] = uv.top;

					vertices[8] = x2;
					vertices[9] = y2;

					vertices[10] = uv.right;
					vertices[11] = uv.bottom;

					vertices[12] = x1;
					vertices[13] = y2;

					vertices[14] = uv.left;
					vertices[15] = uv.bottom;

				} else {

					//If we don't need to draw this tile simply set the quad to size 0 at 0, 0.
					// This does result in the quad being drawn, but we are skipping all
					// pixel-filling. This is better than fully skipping rendering as we
					// don't need to manage a buffer of drawable tiles with insertions/deletions.
					Arrays.fill(vertices, 0);
				}

				quads.put(vertices);

				pos++;
				x1 = x2;
				x2 += cellW;

			}

			y1 = y2;
			y2 += cellH;
		}

	}

	//private int camX, camY, camW, camH;
	//private int topLeft, bottomRight, length;

	@Override
	public void draw() {

		super.draw();

		if (!updated.isEmpty()) {
			updateVertices();
			if (buffer == null)
				buffer = new Vertexbuffer(quads);
			else {
				if (fullUpdate) {
					buffer.updateVertices(quads);
					fullUpdate = false;
				} else {
					buffer.updateVertices(quads,
							topLeftUpdating * 16,
							bottomRightUpdating * 16);
				}
			}
			topLeftUpdating = -1;
			LastUpdating = updating;
			updating.setEmpty();
		}

		NoosaScript script = script();

		texture.bind();

		script.uModel.valueM4( matrix );
		script.lighting(
				rm, gm, bm, am,
				ra, ga, ba, aa );

		script.camera( camera );

		script.drawQuadSet( buffer, size, 0 ); //mod: rendering

	}
	
	protected NoosaScript script(){
		return NoosaScriptNoLighting.get();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (buffer != null)
			buffer.delete();
	}
	/*
	protected boolean needsRender(int pos){
		return data[pos] >= 0;
	} //mod: THIS?
	*/

	protected boolean needsRender(int pos){

		TileX = pos % mapWidth;
		TileY = pos / mapWidth;

		return (Math.abs(TileX / ChunkSize - HeroX / ChunkSize) < 2) && (Math.abs(TileY / ChunkSize - HeroY / ChunkSize) < 2);
	} //mod: TODO chunk rendering

}



