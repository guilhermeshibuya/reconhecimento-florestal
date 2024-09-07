package com.example.reconhecimentoflorestal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ImageTestAutomation {
    private final Context context;
    private File resultFile;
    private String[] classes = {
            "Acrocarpus fraxinifolius_ACROCARPUS", "Apuleia leiocarpa_GARAPEIRA", "Araucaria angustifolia_ARAUCARIA", "Aspidosperma polyneuron_PEROBA ROSA", "Aspidosperma sp_PAU CETIM", "Bagassa guianensis_TATAJUBA", "Balfourodendron riedelianum_PAU MARFIM", "Bertholletia excelsa_CASTANHEIRA", "Bowdichia sp_SUCUPIRA", "Brosimum paraensis_MUIRAPIRANGA", "Carapa guianensis_ANDIROBA", "Cariniana estrellensis_JEQUITIBA", "Cedrela fissilis_CEDRO", "Cedrelinga catenaeformis_CEDRORANA", "Clarisia racemosa_GUARIUBA", "Cordia Goeldiana_FREIJO", "Cordia alliodora_LOURO-AMARELO", "Couratari sp_TAUARI", "Dipteryx sp_CUMARU", "Erisma uncinatum_CEDRINHO", "Eucalyptus sp_EUCALIPTO", "Euxylophora paraensis_PAU AMARELO", "Goupia glabra_CUPIUBA", "Grevilea robusta_GREVILEA", "Handroanthus sp_IPE", "Hymenaea sp_JATOBA", "Hymenolobium petraeum_ANGELIM PEDRA", "Laurus nobilis_LOURO", "Machaerium sp_MACHAERIUM", "Manilkara huberi_MASSARANDUBA", "Melia azedarach_CINAMOMO", "Mezilaurus itauba_ITAUBA", "Micropholis venulosa_CURUPIXA", "Mimosa scabrella_BRACATINGA", "Myroxylon balsamum_CABREUVA VERMELHA", "Ocotea porosa_IMBUIA", "Peltagyne sp_ROXINHO", "Pinus sp_PINUS", "Podocarpus lambertii_PODOCARPUS", "Pouteria pachycarpa_GOIABAO", "Simarouba amara_MARUPA", "Swietenia macrophylla_MOGNO", "Virola surinamensis_VIROLA", "Vochysia sp_QUARUBA CEDRO"
    };

    public ImageTestAutomation(Context context) {
        this.context = context;
        resultFile = new File(Environment.getExternalStorageDirectory(), "classification_results.txt");
    }

    public void run() {
        File imageDir = new File(Environment.getExternalStorageDirectory(), "TestImages");

        if (imageDir.exists() && imageDir.isDirectory()) {
            File[] imageFiles = imageDir.listFiles((dir, name) -> name.endsWith(".jpg") ||  name.endsWith(".JPG") || name.endsWith(".png"));

            if (imageFiles != null) {
                for (File imageFile : imageFiles) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                        if (bitmap != null) {
                            ModelUtilities modelUtilities = new ModelUtilities(context);

                            float[][][][] inputArray = modelUtilities.preprocessImages(bitmap);

                            InferenceResult results = modelUtilities.runInference(inputArray);

                            StringBuilder resultBuilder = new StringBuilder();
                            for (int i = 0; i < results.results.length; i++) {
                                float percentage = results.results[i] * 100;
                                String name = classes[results.indices[i]];
                                resultBuilder.append(String.format("Classe: %s, Confiança: %.6f%%", name, percentage));
                                resultBuilder.append("\n");
                            }
                            saveResult(imageFile.getName(), resultBuilder.toString());
                            bitmap.recycle();
                        }
                    } catch (Exception e) {
                        Log.e("ImageTestAutomation", "Erro ao processar a imagem: " + imageFile.getName(), e);
                    }
                }
                Log.d("Classificacao", "FInalizou");
            } else {
                Log.e("ImageTestAutomation", "Nenhuma imagem encontrada no diretório.");
            }
        } else {
            Log.e("ImageTestAutomation", "Diretório de imagens de teste não encontrado.");
        }
    }

    private void saveResult(String imageName, String result) {
        BufferedWriter writer = null;
        try {
            // Abrir o arquivo para escrita no modo append
            writer = new BufferedWriter(new FileWriter(resultFile, true));
            writer.write("Imagem: " + imageName);
            writer.newLine();
            writer.write(result);
            writer.newLine(); // Adiciona uma nova linha após cada resultado
            writer.newLine(); // Separação entre resultados de imagens diferentes
        } catch (IOException e) {
            Log.e("ImageTestAutomation", "Erro ao escrever no arquivo de resultados.", e);
        } finally {
            // Fechar o BufferedWriter para garantir que o conteúdo seja salvo corretamente
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e("ImageTestAutomation", "Erro ao fechar o arquivo de resultados.", e);
                }
            }
        }
    }
}
