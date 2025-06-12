package com.example.lab6_20206331.fragments;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
        if (btnSelectMonth != null) {
            btnSelectMonth.setOnClickListener(v -> showMonthPicker());
        }
    }

    private void showMonthPicker() {
        // VERIFICAR CONTEXTO ANTES DE CREAR DATEPICKER
        if (getContext() == null) {
            Log.w(TAG, "Contexto nulo, no se puede mostrar DatePicker");
            return;
        }

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
        if (tvSelectedMonth != null && selectedCalendar != null) {
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            String monthYear = monthFormat.format(selectedCalendar.getTime());
            tvSelectedMonth.setText(monthYear);
        }
    }

    private void loadDataForSelectedMonth() {
        showProgress(true);

        String selectedMonthYear = getSelectedMonthYear();

        // Cargar ingresos
        if (ingresoRepository != null) {
            ingresoRepository.getAllIngresos(new IngresoRepository.OnIngresosLoadedListener() {
                @Override
                public void onSuccess(List<Ingreso> ingresos) {
                    // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment no está adjunto, ignorando callback de ingresos");
                        return;
                    }

                    ingresosList = filterByMonth(ingresos, selectedMonthYear);

                    // Cargar egresos
                    if (egresoRepository != null) {
                        egresoRepository.getAllEgresos(new EgresoRepository.OnEgresosLoadedListener() {
                            @Override
                            public void onSuccess(List<Egreso> egresos) {
                                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                                if (!isAdded() || getContext() == null) {
                                    Log.w(TAG, "Fragment no está adjunto, ignorando callback de egresos");
                                    return;
                                }

                                egresosList = filterByMonth(egresos, selectedMonthYear);

                                // Calcular totales y actualizar UI
                                calculateTotals();
                                updateChartsAndSummary();
                                showProgress(false);
                            }

                            @Override
                            public void onError(String error) {
                                // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                                if (!isAdded() || getContext() == null) {
                                    Log.w(TAG, "Fragment no está adjunto, ignorando callback de error egresos");
                                    return;
                                }

                                showProgress(false);
                                showError("Error cargando egresos: " + error);
                            }
                        });
                    } else {
                        showProgress(false);
                        showError("Repository de egresos no disponible");
                    }
                }

                @Override
                public void onError(String error) {
                    // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment no está adjunto, ignorando callback de error ingresos");
                        return;
                    }

                    showProgress(false);
                    showError("Error cargando ingresos: " + error);
                }
            });
        } else {
            showProgress(false);
            showError("Repository de ingresos no disponible");
        }
    }

    private String getSelectedMonthYear() {
        if (selectedCalendar != null) {
            SimpleDateFormat format = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            return format.format(selectedCalendar.getTime());
        }
        return "";
    }

    private <T> List<T> filterByMonth(List<T> items, String monthYear) {
        List<T> filtered = new ArrayList<>();

        if (items == null || monthYear == null || monthYear.isEmpty()) {
            return filtered;
        }

        for (T item : items) {
            String itemDate = "";

            if (item instanceof Ingreso) {
                itemDate = ((Ingreso) item).getFecha();
            } else if (item instanceof Egreso) {
                itemDate = ((Egreso) item).getFecha();
            }

            // Extraer MM/yyyy de la fecha dd/MM/yyyy
            if (itemDate != null && itemDate.length() >= 10) {
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

        if (ingresosList != null) {
            for (Ingreso ingreso : ingresosList) {
                if (ingreso != null) {
                    totalIngresos += ingreso.getMonto();
                }
            }
        }

        if (egresosList != null) {
            for (Egreso egreso : egresosList) {
                if (egreso != null) {
                    totalEgresos += egreso.getMonto();
                }
            }
        }

        balance = totalIngresos - totalEgresos;
    }

    private void updateChartsAndSummary() {
        // VERIFICAR QUE EL FRAGMENT SIGUE ADJUNTO
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment no está adjunto, no actualizando UI");
            return;
        }

        updateSummaryCards();
        updatePieChart();
        updateBarChart();
    }

    private void updateSummaryCards() {
        if (tvTotalIngresos == null || tvTotalEgresos == null || tvBalance == null) {
            Log.w(TAG, "TextViews no inicializados, saltando actualización de summary cards");
            return;
        }

        DecimalFormat formatter = new DecimalFormat("#,##0.00");

        tvTotalIngresos.setText("S/. " + formatter.format(totalIngresos));
        tvTotalEgresos.setText("S/. " + formatter.format(totalEgresos));
        tvBalance.setText("S/. " + formatter.format(balance));

        // Cambiar color del balance según sea positivo o negativo
        try {
            if (balance >= 0) {
                tvBalance.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cambiando color del balance", e);
        }
    }

    private void updatePieChart() {
        if (pieChart == null) {
            Log.w(TAG, "PieChart no inicializado, saltando actualización");
            return;
        }

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

        try {
            pieChart.setData(slices);
        } catch (Exception e) {
            Log.e(TAG, "Error actualizando pie chart", e);
        }
    }

    private void updateBarChart() {
        if (barChart == null) {
            Log.w(TAG, "BarChart no inicializado, saltando actualización");
            return;
        }

        try {
            // Usar el método especializado para datos financieros
            barChart.setFinancialData((float) totalIngresos, (float) totalEgresos);
        } catch (Exception e) {
            Log.e(TAG, "Error actualizando bar chart", e);
        }
    }

    private void showProgress(boolean show) {
        if (show && isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Cargando datos...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        // VERIFICACIÓN SEGURA PARA TOAST
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "❌ " + message, Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "Error (fragment no adjunto): " + message);
        }
    }

    private void showInfo(String message) {
        // VERIFICACIÓN SEGURA PARA TOAST
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "ℹ️ " + message, Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "Info (fragment no adjunto): " + message);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Limpiar repositories si es necesario
        if (ingresoRepository != null) {
            ingresoRepository.cleanup();
        }
        if (egresoRepository != null) {
            egresoRepository.cleanup();
        }
    }
}