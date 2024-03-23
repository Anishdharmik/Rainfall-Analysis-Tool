import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/weather";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Md112233";

    private static Connection connection = null;

    public static void main(String[] args) {
        initializeDatabase();

        JFrame frame = new JFrame("Rainfall Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.add(new JLabel("Number of years: "));
        JTextField yearsField = new JTextField();
        inputPanel.add(yearsField);

        inputPanel.add(new JLabel("Data from year: "));
        JTextField year1Field = new JTextField();
        inputPanel.add(year1Field);

        inputPanel.add(new JLabel("To year: "));
        JTextField year2Field = new JTextField();
        inputPanel.add(year2Field);

        JButton calculateButton = new JButton("Calculate");
        inputPanel.add(calculateButton);

        frame.add(inputPanel, BorderLayout.NORTH);

        JTextArea outputArea = new JTextArea();
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int noOfYears = Integer.parseInt(yearsField.getText());
                int year1 = Integer.parseInt(year1Field.getText());
                int year2 = Integer.parseInt(year2Field.getText());

                float[][] yearRain = new float[noOfYears][12];

                for (int years = 0; years < noOfYears; years++) {
                    for (int months = 0; months < 12; months++) {
                        String prompt = "Enter rainfall for Year " + (year1 + years) + ", Month " + (months + 1) + ": ";
                        yearRain[years][months] = Float.parseFloat(JOptionPane.showInputDialog(prompt));
                    }
                }

                StringBuilder output = new StringBuilder();
                output.append("YEAR\t\tRAINFALL (INCHES)\n");

                float total = 0;

                for (int years = 0; years < noOfYears; years++) {
                    float sum = 0;
                    for (int months = 0; months < 12; months++) {
                        sum += yearRain[years][months];
                        insertRainfallData(year1 + years, months + 1, yearRain[years][months]);
                    }
                    output.append(String.format("%5d   %15.1f%n", year1 + years, sum));
                    total += sum;
                }

                float yearlyAverage = total / noOfYears;
                output.append(String.format("%nThe yearly average is: %.2f inches.%n%n", yearlyAverage));

                float[] monthlyAverages = new float[12];

                for (int months = 0; months < 12; months++) {
                    float sum = 0;
                    for (int years = 0; years < noOfYears; years++) {
                        sum += yearRain[years][months];
                    }
                    float monthlyAverage = sum / noOfYears;
                    monthlyAverages[months] = monthlyAverage;
                    output.append(String.format("%4.1f ", monthlyAverage));
                }

                insertAverageYearlyRainfall(year1, year2, yearlyAverage);
                insertAverageMonthlyRainfall(monthlyAverages);

                outputArea.setText(output.toString());
            }
        });

        frame.setSize(600, 400);
        frame.setVisible(true);
    }

    private static void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        String createRainfallDataTable = "CREATE TABLE IF NOT EXISTS rainfall_data (" +
                "year INT, " +
                "month INT, " +
                "rainfall FLOAT, " +
                "PRIMARY KEY (year, month))";

        String createAverageYearlyTable = "CREATE TABLE IF NOT EXISTS average_yearly_rainfall (" +
                "year INT PRIMARY KEY, " +
                "average_rainfall FLOAT)";

        String createAverageMonthlyTable = "CREATE TABLE IF NOT EXISTS average_monthly_rainfall (" +
                "month INT PRIMARY KEY, " +
                "average_rainfall FLOAT)";

        try (PreparedStatement statement = connection.prepareStatement(createRainfallDataTable)) {
            statement.execute();
        }

        try (PreparedStatement statement = connection.prepareStatement(createAverageYearlyTable)) {
            statement.execute();
        }

        try (PreparedStatement statement = connection.prepareStatement(createAverageMonthlyTable)) {
            statement.execute();
        }
    }

    private static void insertRainfallData(int year, int month, float rainfall) {
        String insertDataQuery = "INSERT INTO rainfall_data (year, month, rainfall) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertDataQuery)) {
            statement.setInt(1, year);
            statement.setInt(2, month);
            statement.setFloat(3, rainfall);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    

    private static void insertAverageYearlyRainfall(int startYear, int endYear, float average) {
        String insertQuery = "INSERT INTO average_yearly_rainfall (year, average_rainfall) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
            statement.setInt(1, endYear);
            statement.setFloat(2, average);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insertAverageMonthlyRainfall(float[] monthlyAverages) {
        String insertQuery = "INSERT INTO average_monthly_rainfall (month, average_rainfall) VALUES (?, ?)";
        for (int month = 0; month < 12; month++) {
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.setInt(1, month + 1);
                statement.setFloat(2, monthlyAverages[month]);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
