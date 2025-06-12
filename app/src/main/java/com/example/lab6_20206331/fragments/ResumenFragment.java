package com.example.lab6_20206331.fragments;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lab6_20206331.R;
import com.example.lab6_20206331.FirebaseUtil;
import com.example.lab6_20206331.models.Egreso;
import com.example.lab6_20206331.models.Ingreso;
import com.example.lab6_20206331.repository.EgresoRepository;
import com.example.lab6_20206331.repository.IngresoRepository;
import com.example.lab6_20206331.views.BarChartView;
import com.example.lab6_20206331.views.PieChartView;
import android.widget.Button;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ResumenFragment extends Fragment {

    private static final String TAG = "ResumenFragment";

    // Views
    private TextView tvSelectedMonth;
    private Button btnSelectMonth;
    private PieChartView pieChart;
    private BarChartView barChart;
    private TextView tvTotalIngresos;
    private TextView tvTotalEgresos;
    private TextView tvBalance;

    // Data
    private IngresoRepository ingresoRepository;
    private EgresoRepository egresoRepository;
    private Calendar selectedCalendar;
    private List<Ingreso> ingresosList;
    private List<Egreso> egresosList;

    // Financial data
    private double totalIngresos = 0.0;
    private double totalEgresos = 0.0;
    private double balance = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resumen, container, false);

        initViews(view);
        initRepositories();
        initSelectedMonth();
        setupClickListeners();
        loadDataForSelectedMonth();

        return view;
    }

    private void initViews(View view) {
        tvSelectedMonth = view.findViewById(R.id.tv_selected_month);
        btnSelectMonth = view.findViewById(R.id.btn_select_month);
        pieChart = view.findViewById(R.id.pie_chart);
        barChart = view.findViewById(R.id.bar_chart);
        tvTotalIngresos = view.findViewById(R.id.tv_total_ingresos);
        tvTotalEgresos = view.findViewById(R.id.tv_total_egresos);
        tvBalance = view.findViewById(R.id.tv_balance);
    }

    private void initRepositories() {
        ingresoRepository = IngresoRepository.getInstance();
        egresoRepository = EgresoRepository.getInstance();
        ingresosList = new ArrayList<>();
        egresosList = new ArrayList<>();
    }

    private void initSelectedMonth() {
        selectedCalendar = Calendar.getInstance();
        updateSelectedMonthDisplay();
    }

    private void setupClickListeners() {
        btnSelectMonth.setOnClickListener(v -> showMonthPicker());
    }

    private void showMonthPicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, 1); // Primer día del mes

                    updateSelectedMonthDisplay();
                    loadDataForSelectedMonth();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );

        // Configurar para mostrar solo mes y año
        datePickerDialog.getDatePicker().setSpinnersShown(true);
        datePickerDialog.getDatePicker().setCalendarViewShown(false);
        datePickerDialog.setTitle("Seleccionar Mes");

        datePickerDialog.show();
    }

    private void updateSelectedMonthDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = monthFormat.format(selectedCalendar.getTime());
        tvSelectedMonth.setText(monthYear);
    }

    private void loadDataForSelectedMonth() {
        showProgress(true);

        String selectedMonthYear = getSelectedMonthYear();

        // Cargar ingresos
        ingresoRepository.getAllIngresos(new IngresoRepository.OnIngresosLoadedListener() {
            @Override
            public void onSuccess(List<Ingreso> ingresos) {
                ingresosList = filterByMonth(ingresos, selectedMonthYear);

                // Cargar egresos
                egresoRepository.getAllEgresos(new EgresoRepository.OnEgresosLoadedListener() {
                    @Override
                    public void onSuccess(List<Egreso> egresos) {
                        egresosList = filterByMonth(egresos, selectedMonthYear);

                        // Calcular totales y actualizar UI
                        calculateTotals();
                        updateChartsAndSummary();
                        showProgress(false);
                    }

                    @Override
                    public void onError(String error) {
                        showProgress(false);
                        showError("Error cargando egresos: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                showProgress(false);
                showError("Error cargando ingresos: " + error);
            }
        });
    }

    private String getSelectedMonthYear() {
        SimpleDateFormat format = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        return format.format(selectedCalendar.getTime());
    }

    private <T> List<T> filterByMonth(List<T> items, String monthYear) {
        List<T> filtered = new ArrayList<>();

        for (T item : items) {
            String itemDate = "";

            if (item instanceof Ingreso) {
                itemDate = ((Ingreso) item).getFecha();
            } else if (item instanceof Egreso) {
                itemDate = ((Egreso) item).getFecha();
            }

            // Extraer MM/yyyy de la fecha dd/MM/yyyy
            if (itemDate.length() >= 10) {
                String itemMonthYear = itemDate.substring(3); // "MM/yyyy"
                if (itemMonthYear.equals(monthYear)) {
                    filtered.add(item);
                }
            }
        }

        return filtered;
    }

    private void calculateTotals() {
        totalIngresos = 0.0;
        totalEgresos = 0.0;

        for (Ingreso ingreso : ingresosList) {
            totalIngresos += ingreso.getMonto();
        }

        for (Egreso egreso : egresosList) {
            totalEgresos += egreso.getMonto();
        }

        balance = totalIngresos - totalEgresos;
    }

    private void updateChartsAndSummary() {
        updateSummaryCards();
        updatePieChart();
        updateBarChart();
    }

    private void updateSummaryCards() {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");

        tvTotalIngresos.setText("S/. " + formatter.format(totalIngresos));
        tvTotalEgresos.setText("S/. " + formatter.format(totalEgresos));
        tvBalance.setText("S/. " + formatter.format(balance));

        // Cambiar color del balance según sea positivo o negativo
        if (balance >= 0) {
            tvBalance.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void updatePieChart() {
        List<PieChartView.PieSlice> slices = new ArrayList<>();

        if (totalIngresos > 0 || totalEgresos > 0) {
            // Calcular porcentajes con respecto al total
            double total = totalIngresos + totalEgresos;

            if (totalIngresos > 0) {
                float ingresosPorcentaje = (float) totalIngresos;
                slices.add(new PieChartView.PieSlice(
                        ingresosPorcentaje,
                        Color.parseColor("#4CAF50"), // Verde para ingresos
                        "Ingresos"
                ));
            }

            if (totalEgresos > 0) {
                float egresosPorcentaje = (float) totalEgresos;
                slices.add(new PieChartView.PieSlice(
                        egresosPorcentaje,
                        Color.parseColor("#F44336"), // Rojo para egresos
                        "Egresos"
                ));
            }
        } else {
            // Sin datos
            slices.add(new PieChartView.PieSlice(
                    100f,
                    Color.parseColor("#9E9E9E"), // Gris para sin datos
                    "Sin datos"
            ));
        }

        pieChart.setData(slices);
    }

    private void updateBarChart() {
        // Usar el método especializado para datos financieros
        barChart.setFinancialData((float) totalIngresos, (float) totalEgresos);
    }

    private void showProgress(boolean show) {
        if (show) {
            Toast.makeText(getContext(), "Cargando datos...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_LONG).show();
    }

    private void showInfo(String message) {
        Toast.makeText(getContext(), "ℹ️ " + message, Toast.LENGTH_SHORT).show();
    }
}