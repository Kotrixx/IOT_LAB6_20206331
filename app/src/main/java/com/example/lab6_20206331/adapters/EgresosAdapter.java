package com.example.lab6_20206331.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20206331.R;
import com.example.lab6_20206331.models.Egreso;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EgresosAdapter extends RecyclerView.Adapter<EgresosAdapter.EgresoViewHolder> {

    private List<Egreso> egresosList;
    private OnEgresoClickListener editListener;
    private OnEgresoClickListener deleteListener;

    public interface OnEgresoClickListener {
        void onEgresoClick(Egreso egreso);
    }

    public EgresosAdapter(List<Egreso> egresosList, OnEgresoClickListener editListener, OnEgresoClickListener deleteListener) {
        this.egresosList = egresosList;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public EgresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_egreso, parent, false);
        return new EgresoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EgresoViewHolder holder, int position) {
        Egreso egreso = egresosList.get(position);
        holder.bind(egreso);
    }

    @Override
    public int getItemCount() {
        return egresosList.size();
    }

    public void updateList(List<Egreso> newList) {
        this.egresosList = newList;
        notifyDataSetChanged();
    }

    class EgresoViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitulo;
        private TextView tvMonto;
        private TextView tvDescripcion;
        private TextView tvFecha;
        private TextView tvFechaRelativa;
        private ImageView ivEdit;
        private ImageView ivDelete;

        public EgresoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_titulo);
            tvMonto = itemView.findViewById(R.id.tv_monto);
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            tvFechaRelativa = itemView.findViewById(R.id.tv_fecha_relativa);
            ivEdit = itemView.findViewById(R.id.iv_edit);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }

        public void bind(Egreso egreso) {
            // Título
            tvTitulo.setText(egreso.getTitulo());

            // Formatear el monto como moneda peruana con signo negativo
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
            try {
                formatter.setCurrency(java.util.Currency.getInstance("PEN"));
            } catch (Exception e) {
                // Si no se puede establecer PEN, usar el formato por defecto
            }

            String formattedAmount = formatter.format(egreso.getMonto()).replace("PEN", "S/.");
            // Agregar signo negativo para indicar que es un egreso
            tvMonto.setText("-" + formattedAmount);

            // Descripción - mostrar u ocultar según el diseño
            if (egreso.getDescripcion() != null && !egreso.getDescripcion().trim().isEmpty()) {
                tvDescripcion.setText(egreso.getDescripcion());
                tvDescripcion.setVisibility(View.VISIBLE);
            } else {
                tvDescripcion.setVisibility(View.GONE);
            }

            // Fecha
            tvFecha.setText(egreso.getFecha());

            // Fecha relativa (como en tu diseño)
            if (tvFechaRelativa != null) {
                String relativeDate = getRelativeDate(egreso.getFecha());
                if (relativeDate != null) {
                    tvFechaRelativa.setText("• " + relativeDate);
                    tvFechaRelativa.setVisibility(View.VISIBLE);
                } else {
                    tvFechaRelativa.setVisibility(View.GONE);
                }
            }

            // Click listeners para botones de acción
            ivEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEgresoClick(egreso);
                }
            });

            ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onEgresoClick(egreso);
                }
            });

            // Click en toda la tarjeta para editar (opcional)
            itemView.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEgresoClick(egreso);
                }
            });
        }

        private String getRelativeDate(String dateString) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date egresoDate = sdf.parse(dateString);
                Date today = new Date();

                if (egresoDate == null) return null;

                long diffInMillies = today.getTime() - egresoDate.getTime();
                long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);

                if (diffInDays == 0) return "Hoy";
                if (diffInDays == 1) return "Ayer";
                if (diffInDays == -1) return "Mañana";
                if (diffInDays > 1 && diffInDays <= 7) return "Hace " + diffInDays + " días";
                if (diffInDays < -1 && diffInDays >= -7) return "En " + Math.abs(diffInDays) + " días";

                return null; // Para fechas más lejanas, no mostrar fecha relativa
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}