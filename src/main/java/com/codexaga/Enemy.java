package com.codexaga;

public final class Enemy
{
    private final int row;
    private final int col;
    private final EnemyKind kind;

    private boolean alive = true;
    private EnemyMode mode = EnemyMode.FORMATION;
    private boolean carryingCapturedShip;
    private double x;
    private double y;
    private double timer;
    private double diveDirection;
    private double targetX;

    public Enemy(int row, int col, EnemyKind kind)
    {
        this.row = row;
        this.col = col;
        this.kind = kind;
    }

    public int getRow()
    {
        return row;
    }

    public int getCol()
    {
        return col;
    }

    public EnemyKind getKind()
    {
        return kind;
    }

    public boolean isAlive()
    {
        return alive;
    }

    public void kill()
    {
        alive = false;
        carryingCapturedShip = false;
    }

    public EnemyMode getMode()
    {
        return mode;
    }

    public void setMode(EnemyMode mode)
    {
        this.mode = mode;
    }

    public boolean isCarryingCapturedShip()
    {
        return carryingCapturedShip;
    }

    public void setCarryingCapturedShip(boolean carryingCapturedShip)
    {
        this.carryingCapturedShip = carryingCapturedShip;
    }

    public double getX()
    {
        return x;
    }

    public void setX(double x)
    {
        this.x = x;
    }

    public double getY()
    {
        return y;
    }

    public void setY(double y)
    {
        this.y = y;
    }

    public double getTimer()
    {
        return timer;
    }

    public void setTimer(double timer)
    {
        this.timer = timer;
    }

    public void advanceTimer(double delta)
    {
        timer += delta;
    }

    public double getDiveDirection()
    {
        return diveDirection;
    }

    public void setDiveDirection(double diveDirection)
    {
        this.diveDirection = diveDirection;
    }

    public double getTargetX()
    {
        return targetX;
    }

    public void setTargetX(double targetX)
    {
        this.targetX = targetX;
    }

    public boolean isInFormation()
    {
        return mode == EnemyMode.FORMATION;
    }
}
