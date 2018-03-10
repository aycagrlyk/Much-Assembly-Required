package net.simon987.server.game;

import com.mongodb.DBObject;
import net.simon987.server.GameServer;
import net.simon987.server.io.GameObjectDeserializer;
import net.simon987.server.io.JSONSerialisable;
import net.simon987.server.io.MongoSerialisable;
import net.simon987.server.plugin.ServerPlugin;
import org.json.simple.JSONObject;

import java.awt.*;
import java.util.ArrayList;

/**
 * An INSTANCE of an object (e.g. a Tree, a character ...) inside the
 * game universe
 */
public abstract class GameObject implements JSONSerialisable, MongoSerialisable {

    private boolean dead;
    /**
     * Object's unique identifier
     */
    private long objectId;

    /**
     * X coordinate of the object in its World
     */
    private int x;

    /**
     * Y coordinate of the object in its World
     */
    private int y;

    /**
     * Direction of the object
     */
    private Direction direction = Direction.NORTH;

    /**
     * Current World of the object
     */
    private World world;


    /**
     * Increment the location of the game object by 1 tile
     * Collision checks happen here
     */
    public boolean incrementLocation() {

        int newX = getX() + direction.dX;
        int newY = getY() + direction.dY;

        if (newX < 0 || newY < 0 || newX >= world.getWorldSize() || newY >= world.getWorldSize()) {
            //Next tile is out of world bounds, move to next world
            World nextWorld = GameServer.INSTANCE.getGameUniverse().getWorld(
                    world.getX() + direction.dX, world.getY() + direction.dY, true, world.getDimension());

            //Move object to next World
            world.removeObject(this);
            world.decUpdatable();
            nextWorld.addObject(this);
            nextWorld.incUpdatable();
            setWorld(nextWorld);

            //Set position on next World according to direction
            switch (direction) {
                case NORTH:
                    setY(nextWorld.getWorldSize() - 1);
                    break;
                case EAST:
                    setX(0);
                    break;
                case SOUTH:
                    setY(0);
                    break;
                case WEST:
                    setX(nextWorld.getWorldSize() - 1);
                    break;
            }

            return true;

        } else {

            ArrayList<GameObject> frontObjects = world.getGameObjectsAt(newX, newY);

            //Check for enterable objects
            if (frontObjects.size() > 0 && frontObjects.get(0) instanceof Enterable) {
                return (((Enterable) frontObjects.get(0)).enter(this));
            }

            //Check collision
            if (!world.isTileBlocked(newX, newY)) { //Check for collision
                //Tile is passable
                x = newX;
                y = newY;
                return true;
            } else {
                return false;
            }

        }

    }

    public abstract char getMapInfo();

    public Point getFrontTile() {

        if (direction == Direction.NORTH) {
            return new Point(x, y - 1);
        } else if (direction == Direction.EAST) {
            return new Point(x + 1, y);
        } else if (direction == Direction.SOUTH) {
            return new Point(x, y + 1);
        } else {
            return new Point(x - 1, y);
        }

    }

    /**
     * Get the first directly adjacent tile (starting east, going clockwise)
     */
    public Point getAdjacentTile() {

        if (!getWorld().isTileBlocked(getX() + 1, getY())) {
            return new Point(getX() + 1, getY());

        } else if (!getWorld().isTileBlocked(getX(), getY() + 1)) {
            return new Point(getX(), getY() + 1);

        } else if (!getWorld().isTileBlocked(getX() - 1, getY())) {
            return new Point(getX() - 1, getY());

        } else if (!getWorld().isTileBlocked(getX(), getY() - 1)) {
            return new Point(getX(), getY() - 1);
        } else {
            return null;
        }
    }

    public int getAdjacentTileCount(boolean diagonals) {

        int count = 0;

        if (!getWorld().isTileBlocked(getX() + 1, getY())) {
            count++;
        }
        if (!getWorld().isTileBlocked(getX(), getY() + 1)) {
            count++;
        }
        if (!getWorld().isTileBlocked(getX() - 1, getY())) {
            count++;
        }
        if (!getWorld().isTileBlocked(getX(), getY() - 1)) {
            count++;
        }

        if (diagonals) {
            if (!getWorld().isTileBlocked(getX() + 1, getY() + 1)) {
                count++;
            }
            if (!getWorld().isTileBlocked(getX() - 1, getY() + 1)) {
                count++;
            }
            if (!getWorld().isTileBlocked(getX() + 1, getY() - 1)) {
                count++;
            }
            if (!getWorld().isTileBlocked(getX() - 1, getY() - 1)) {
                count++;
            }
        }

        return count;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public JSONObject serialise() {
        return new JSONObject();
    }

    public static GameObject deserialize(DBObject obj) {
//
        for (ServerPlugin plugin : GameServer.INSTANCE.getPluginManager().getPlugins()) {

            if (plugin instanceof GameObjectDeserializer) {
                GameObject object = ((GameObjectDeserializer) plugin).deserializeObject(obj);

                if (object != null) {
                    return object;
                }
            }
        }

        return null;
    }


    public boolean isAt(int x, int y) {
        return this.x == x && this.y == y;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    /**
     * Called before this GameObject is removed from the world - defaults to doing nothing
     * @return true if cancelled
     */
    public boolean onDeadCallback() {
        return false;
    }

    public void initialize() {

    }
}