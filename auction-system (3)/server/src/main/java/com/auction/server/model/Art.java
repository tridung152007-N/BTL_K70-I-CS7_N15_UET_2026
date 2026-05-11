package com.auction.server.model;

public class Art extends Item {
    private String artist;
    private int year;

    public Art() { this.category = "ART"; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + name + " | artist=" + artist + " (" + year + ")");
    }
}
