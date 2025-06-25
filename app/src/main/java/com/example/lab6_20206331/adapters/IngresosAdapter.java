package com.example.lab6_20206331.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20206331.R;
import com.example.lab6_20206331.models.Ingreso;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class IngresosAdapter extends RecyclerView.Adapter<IngresosAdapter.IngresoViewHolder> {

    private List<Ingreso> ingresosList;
    private OnIngresoClickListener editListener;
    private OnIngresoClickListener deleteListener;
    private OnIngresoClickListener downloadListener; // NUEVO

    public interface OnIngresoClickListener {
        void onIngresoClick(Ingreso ingreso);
    }

    // Constructor actualizado
    public IngresosAdapter(List<Ingreso> ingresosList,
                           OnIngresoClickListener editListener,
                           OnIngresoClickListener deleteListener,
                           OnIngresoClickListener downloadListener) { // NUEVO PARÁMETRO
        this.ingresosList = ingresosList;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.downloadListener = downloadListener; // NUEVO
    }

    // Constructor de compatibilidad (sin descarga)
    public IngresosAdapter(List<Ingreso> ingresosList,
                           OnIngresoClickListener editListener,
                           OnIngresoClickListener deleteListener) {
        this.ingresosList = ingresosList;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.downloadListener = null;
    }

    @NonNull
    @Override
    public IngresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingreso, parent, false);
        return new IngresoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngresoViewHolder holder, int position) {
        Ingreso ingreso = ingresosList.get(position);
        holder.bind(ingreso);
    }

    @Override
    public int getItemCount() {
        return ingresosList.size();
    }

    class IngresoViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitulo;
        private TextView tvMonto;
        private TextView tvDescripcion;
        private TextView tvFecha;
        private ImageView ivEdit;
        private ImageView ivDelete;
        private ImageView ivDownload; // NUEVO
        private ImageView ivComprobante; // NUEVO - Indicador de comprobante

        public IngresoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_titulo);
            tvMonto = itemView.findViewById(R.id.tv_monto);
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion);
            tvFecha = itemView.findViewById(R.id.tv_fecha);
            ivEdit = itemView.findViewById(R.id.iv_edit);
            ivDelete = itemView.findViewById(R.id.iv_delete);

            // NUEVOS CAMPOS (agregar estos IDs a tu layout)
            ivDownload = itemView.findViewById(R.id.iv_download);
            ivComprobante = itemView.findViewById(R.id.iv_comprobante);
        }

        public void bind(Ingreso ingreso) {
            tvTitulo.setText(ingreso.getTitulo());

            // Formatear el monto como moneda
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
            tvMonto.setText(formatter.format(ingreso.getMonto()));

            // Mostrar descripción o texto por defecto
            if (ingreso.getDescripcion() != null && !ingreso.getDescripcion().trim().isEmpty()) {
                tvDescripcion.setText(ingreso.getDescripcion());
                tvDescripcion.setVisibility(View.VISIBLE);
            } else {
                tvDescripcion.setVisibility(View.GONE);
            }

            tvFecha.setText(ingreso.getFecha());

            // NUEVAS FUNCIONALIDADES PARA COMPROBANTE
            if (ingreso.hasComprobante()) {
                // Mostrar indicador de comprobante
                if (ivComprobante != null) {
                    ivComprobante.setVisibility(View.VISIBLE);
                    ivComprobante.setImageResource(R.drawable.ic_attachment); // Agregar este icono
                }

                // Mostrar botón de descarga
                if (ivDownload != null) {
                    ivDownload.setVisibility(View.VISIBLE);
                    ivDownload.setOnClickListener(v -> {
                        if (downloadListener != null) {
                            downloadListener.onIngresoClick(ingreso);
                        }
                    });
                }
            } else {
                // Ocultar indicadores si no hay comprobante
                if (ivComprobante != null) {
                    ivComprobante.setVisibility(View.GONE);
                }
                if (ivDownload != null) {
                    ivDownload.setVisibility(View.GONE);
                }
            }

            // Click listeners existentes
            ivEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onIngresoClick(ingreso);
                }
            });

            ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onIngresoClick(ingreso);
                }
            });
        }
    }

    // Método para actualizar la lista
    public void updateList(List<Ingreso> newList) {
        this.ingresosList = newList;
        notifyDataSetChanged();
    }

    // Métodos para actualizar listeners dinámicamente
    public void setDownloadListener(OnIngresoClickListener downloadListener) {
        this.downloadListener = downloadListener;
        notifyDataSetChanged(); // Refrescar para mostrar/ocultar botones
    }
}