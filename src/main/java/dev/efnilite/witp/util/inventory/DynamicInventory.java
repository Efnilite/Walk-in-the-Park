package dev.efnilite.witp.util.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DynamicInventory {

    private final int row;
    private final ArrayList<Integer> slots;

    // row from 0
    public DynamicInventory(int amountRow, int row) {
        slots = new ArrayList<>(getSlots(amountRow));
        this.row = row;
    }

    public int next() {
        int next = slots.remove(0);
        return next + (row * 9);
    }

    private List<Integer> getSlots(int amountRow) {
        switch (amountRow) {
            case 1:
                return Collections.singletonList(4);
            case 2:
                return Arrays.asList(3, 5);
            case 3:
                return Arrays.asList(3, 4, 5);
            case 4:
                return Arrays.asList(2, 3, 5, 6);
            case 5:
                return Arrays.asList(2, 3, 4, 5, 6);
            case 6:
                return Arrays.asList(1, 2, 3, 5, 6, 7);
            case 7:
                return Arrays.asList(1, 2, 3, 4, 5, 6, 7);
            case 8:
                return Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8);
            case 9:
            default:
                return Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8);
        }
    }

}
