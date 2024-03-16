package comprog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class main {

    public static String[] employee;
    
	public static void main(String[] args) {
		boolean conts = true;
        while (conts) {
            define_data_type();
            if (employee != null) {
                displayEmployeeDetails();
                getWeeklyHoursAndSalary(employee[0]);
            } else {
                System.out.println("No data Found. Please try again");
            }
            System.out.println("------------------------------------------------------------------------");
        }
    }

    public static void reset_data() {
        employee = null;
    }

    public static void define_data_type() {
        reset_data();
        System.out.println("Enter an Employee number:");
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            String userInput = inputReader.readLine();
            String employee_detail = get_employee_details(userInput);
            if (!employee_detail.equals("")) {
                String[] row = employee_detail.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                employee = row;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get_employee_details(String employee_id) {
        String file = "src\\employee_details.csv";
        BufferedReader reader = null;
        String line;
        String employee_found = "";
        try {
            reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (row[0].equals(employee_id)) {
                    employee_found = line;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return employee_found;
    }

    public static void displayEmployeeDetails() {
        if (employee.length >= 18) {
            System.out.println("Employee ID: " + employee[0]);
            System.out.println("Name: " + employee[1] + " " + employee[2]);
            System.out.println("Birthdate: " + employee[3]);
            System.out.println("Hourly Rate: " + employee[18]);
        } else {
            System.out.println("Insufficient data for employee details.");
        }
    }

    public static void getWeeklyHoursAndSalary(String employeeId) {
        // Get date range
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
        String startDate = "";
        String endDate = "";
        try {
            System.out.println("Enter start date (MM/DD/YYYY):");
            startDate = inputReader.readLine();
            System.out.println("Enter end date (MM/DD/YYYY):");
            endDate = inputReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read employee details CSV file to get allowances
        String employeeDetailsFile = "src\\employee_details.csv";
        String[] allowances = readEmployeeAllowances(employeeDetailsFile, employeeId);
        if (allowances == null) {
            System.out.println("Employee details not found.");
            return;
        }
        double riceSubsidy = Double.parseDouble(allowances[14].replace(",", "").replace("\"", ""));
        double phoneAllowance = Double.parseDouble(allowances[15].replace(",", "").replace("\"", ""));
        double clothingAllowance = Double.parseDouble(allowances[16].replace(",", "").replace("\"", ""));


        // Read attendance CSV file
        String attendanceFile = "src\\attendance.csv";
        BufferedReader reader = null;
        String line;
        List<Double> weeklyHoursList = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(attendanceFile));
            System.out.println("Employee ID\tDATE\tTimeIn\tTimeOut\tHoursRendered");
            while ((line = reader.readLine()) != null) {
                String[] record = line.split(",");
                if (record[0].equals(employeeId) && isWithinDateRange(record[3], startDate, endDate)) {
                    double hoursWorked = calculateHoursWorked(record[4], record[5]);
                    System.out.println(record[0] + "\t" + record[3] + "\t" + record[4] + "\t" + record[5] + "\t" + hoursWorked);
                    weeklyHoursList.add(hoursWorked);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Calculate total weekly hours
        double totalWeeklyHours = 0;
        for (double hours : weeklyHoursList) {
            totalWeeklyHours += hours;
        }

        // Display total weekly hours
        System.out.println("Total weekly hours: " + totalWeeklyHours);
        // Display hourly rate
        System.out.println("Hourly Rate: " + allowances[18]);
        // Calculate weekly salary
        double hourlyRate = Double.parseDouble(allowances[18]);
        double weeklySalary = totalWeeklyHours * hourlyRate;
        double totalAllowances = riceSubsidy + phoneAllowance + clothingAllowance;

        // Prorate allowances and deductions
        double proratedPercentage = totalWeeklyHours / 160.0; // Assuming 40 hours per week
        //double proratedAllowances = proratedSalaryPercentage * totalAllowances;
        double proratedPagIbig = proratedPercentage * pagIbigDeduction(weeklySalary);
        double proratedPhilHealth = proratedPercentage * philHealthDeduction(weeklySalary);
        double proratedSss = proratedPercentage * sssDeduction(weeklySalary);
        double proratedIncomeTax = proratedPercentage * incomeTaxDeduction(weeklySalary);

        // Display prorated allowances and deductions
        //System.out.println(+ riceSubsidy); //debug
        //System.out.println("proratedSalaryPercentage " + proratedPercentage); //debug
        System.out.println("Prorated Allowances:");
        System.out.println("Rice Subsidy: " + proratedPercentage * riceSubsidy);
        System.out.println("Phone Allowance: " + proratedPercentage * phoneAllowance);
        System.out.println("Clothing Allowance: " + proratedPercentage * clothingAllowance);

        System.out.println("Prorated Deductions:");
        System.out.println("Pag-ibig: " + proratedPagIbig);
        System.out.println("Philhealth: " + proratedPhilHealth);
        System.out.println("SSS: " + proratedSss);
        System.out.println("Withholding Tax: " + proratedIncomeTax);

        double totalDeductions = proratedPagIbig + proratedPhilHealth + proratedSss + proratedIncomeTax;
        double netWeeklySalary = weeklySalary - totalDeductions;
        System.out.println("Net Weekly Salary: " + netWeeklySalary);
    }

    public static String[] readEmployeeAllowances(String filePath, String employeeId) {
        BufferedReader reader = null;
        String line;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (row[0].equals(employeeId)) {
                    return row;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static double calculateHoursWorked(String startTime, String endTime) {
        if (startTime.equals("0:00") || endTime.equals("0:00")) {
            return 0;
        }
        String[] startParts = startTime.trim().split(":");
        String[] endParts = endTime.trim().split(":");
        int startHour = Integer.parseInt(startParts[0]);
        int startMinute = Integer.parseInt(startParts[1]);
        int endHour = Integer.parseInt(endParts[0]);
        int endMinute = Integer.parseInt(endParts[1]);
        int lunchHour = 1;
        endHour -= lunchHour;
        int graceStartHour = 8;
        int graceStartMinute = 11;
        if (startHour == graceStartHour && startMinute >= graceStartMinute) {
            startHour = graceStartHour;
            startMinute = graceStartMinute;
        }
        int totalMinutesWorked = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
        double hoursWorked = totalMinutesWorked / 60.0;
        return hoursWorked;
    }

    public static boolean isWithinDateRange(String dateStr, String startDateStr, String endDateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        try {
            Date date = dateFormat.parse(dateStr);
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);
            // Adjusted to include start date in the range
            return !date.before(startDate) && !date.after(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static double pagIbigDeduction(double weeklySalary) {
        double deduction = 0;
        if (weeklySalary >= 1000 && weeklySalary <= 1500) {
            deduction = weeklySalary * 0.01;
        } else if (weeklySalary > 1500) {
            deduction = Math.min(weeklySalary * 0.02, 100);
        }
        return deduction;
    }

    public static double philHealthDeduction(double weeklySalary) {
        double deduction = 0;
        // Adjusted to handle different salary ranges
        if ((weeklySalary >= 10000 && weeklySalary <= 60000) || weeklySalary == 60000) {
            deduction = (weeklySalary * 0.03) / 2;
        }
        return deduction;
    }

    public static double sssDeduction(double weeklySalary) {
        double deduction = 0.0;
        // Adjusted to handle different salary ranges
        if (weeklySalary < 3250) {
            deduction = Math.min(weeklySalary * 0.01, 135.00);
        } else if (weeklySalary <= 24750) {
            int range = (int) ((weeklySalary - 3250) / 500);
            double baseContri = 157.50;
            double contriIncrement = 22.50;
            deduction = baseContri + range * contriIncrement;
        } else {
            deduction = 1125.00;
        }
        return deduction;
    }

    public static double incomeTaxDeduction(double weeklySalary) {
        double deduction = 0.00;
        // Adjusted to handle different salary ranges
        if (weeklySalary < 20833.33) {
            deduction = weeklySalary * 0;
        } else if (weeklySalary > 20833.33 && weeklySalary < 33333.33) {
            deduction = (weeklySalary - 20833.33) * 0.15;
        } else if (weeklySalary > 33333.33 && weeklySalary < 66666.66) {
            deduction = (weeklySalary - 33333.33) * 0.20 + 22500;
        } else if (weeklySalary > 66666.66 && weeklySalary < 166666.66) {
            deduction = (weeklySalary - 66666.66) * 0.25 + 102500;
        } else if (weeklySalary > 166666.66 && weeklySalary < 666666.66) {
            deduction = (weeklySalary - 166666.66) * 0.32 + 490000;
        } else {
            deduction = (weeklySalary - 666666.66) * 0.35 + 2202500;
        }
        return deduction;
	}

}
