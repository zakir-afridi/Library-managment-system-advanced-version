package com.library.service;

import com.library.model.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CirculationServiceTest {

    @Test
    void testFineCalculationWhenNotOverdue() {
        Transaction tx = new Transaction();
        tx.setDueDate(LocalDate.now().plusDays(5));
        tx.setReturnDate(null);

        assertEquals(0.0, tx.calculateFine(), 0.001);
        assertFalse(tx.isOverdue());
    }

    @Test
    void testFineCalculationWhenOverdue() {
        Transaction tx = new Transaction();
        tx.setDueDate(LocalDate.now().minusDays(10));
        tx.setReturnDate(null);

        assertTrue(tx.isOverdue());
        assertTrue(tx.calculateFine() > 0.0);
    }
}
