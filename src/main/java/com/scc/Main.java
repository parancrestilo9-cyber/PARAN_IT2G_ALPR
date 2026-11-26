package com.scc;

import com.scc.DAO.DatabaseInitializer;
import com.scc.UI.Authentication;

public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        new Authentication().start();
    }
}