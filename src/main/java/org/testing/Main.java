package org.testing;

import org.testing.Module.Expenses;
import org.testing.Module.OrderReports;
import org.testing.Module.Payments;

public class Main {
    public static void main(String[] args)
    {
        OrderReports.allOrderReports();
        Expenses.allExpensesReportData();
        Payments.allPaymentsReceived();

    }
}