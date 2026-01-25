package com.codexaga;

public final class Enemy
{
    private final int row;
    private final int col;
    private boolean alive = true;

    public Enemy(int row, int col)
    {
        this.row = row;
        this.col = col;
    }

    public int getRow()
    {
        return row;
    }

    public int getCol()
    {
        return col;
    }

    public boolean isAlive()
    {
        return alive;
    }

    public void kill()
    {
        alive = false;
    }
}
