package com.library.model;

public class MeasurementUnit {
    private int unitId;
    private String name;
    private String symbol;
    private String status;

    public MeasurementUnit() {
        this.status = "Active";
    }

    public MeasurementUnit(int unitId, String name, String symbol, String status) {
        this.unitId = unitId;
        this.name = name;
        this.symbol = symbol;
        this.status = status != null ? status : "Active";
    }

    public int getUnitId() { return unitId; }
    public void setUnitId(int unitId) { this.unitId = unitId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return "Active".equalsIgnoreCase(status); }

    @Override
    public String toString() { return name + (symbol != null && !symbol.isBlank() ? " (" + symbol + ")" : ""); }
}
