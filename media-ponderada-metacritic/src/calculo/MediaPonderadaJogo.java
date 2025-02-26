package calculo;

import javax.swing.*;
import java.nio.file.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Desktop;
import java.io.File;

public class MediaPonderadaJogo {
	
    public static void main(String[] args) {
        String nomeJogo = JOptionPane.showInputDialog("Digite o nome do jogo:");
        String dataLancamentoInput = JOptionPane.showInputDialog("Digite a data de lançamento (DD-MM-YYYY):");
        
        // Converte para formato padronizado
        String dataLancamento = formatarData(dataLancamentoInput);
        
        int numPlataformas = Integer.parseInt(JOptionPane.showInputDialog("Digite quantas plataformas tem "
        																					+ "reviews:"));
        
        double somaNotasPonderadas = 0;
        int somaCriticos = 0;
        StringBuilder detalhesPlataformas = new StringBuilder();
        
        for (int i = 1; i <= numPlataformas; i++) {
            String nomePlataforma = JOptionPane.showInputDialog("Digite o nome da Plataforma " + i + ":");
            double nota = Double.parseDouble(JOptionPane.showInputDialog("Digite a nota média no " + 
            																		nomePlataforma + ":"));
            int criticos = Integer.parseInt(JOptionPane.showInputDialog("Digite o número de reviews no " + 
            																		nomePlataforma + ":"));
            
            somaNotasPonderadas += nota * criticos;
            somaCriticos += criticos;
            
            detalhesPlataformas.append("Plataforma: ").append(nomePlataforma)
                .append("\nNota Média: ").append(nota)
                .append("\nNúmero de Reviews: ").append(criticos)
                .append("\n----------------------\n");
        }
        
        double notaFinal = somaNotasPonderadas / somaCriticos;
        
        // Conexão com banco de dados SQLite
        try {
            Class.forName("org.sqlite.JDBC"); // Garante que o driver do SQLite está carregado
            Connection conn = DriverManager.getConnection("jdbc:sqlite:jogos.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS jogos (nome TEXT, data TEXT, nota REAL, detalhes TEXT)");
            
            // Verifica se o jogo já existe no banco
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM jogos WHERE nome = ?");
            checkStmt.setString(1, nomeJogo);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                JOptionPane.showMessageDialog(null, "O jogo '" + nomeJogo + "' já existe no banco de dados.",
                														"Aviso", JOptionPane.WARNING_MESSAGE);
            } else {
                PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO jogos (nome, data, nota, "
                														+ "detalhes) VALUES (?, ?, ?, ?)");
                insertStmt.setString(1, nomeJogo);
                insertStmt.setString(2, dataLancamento);
                insertStmt.setDouble(3, notaFinal);
                insertStmt.setString(4, detalhesPlataformas.toString());
                insertStmt.executeUpdate();
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Página HTML
        try {
            String htmlContent = "<html><head><title>Nota dos Games</title></head><body>" +
                    "<h1>Reviews e Média Ponderada dos Games</h1>" +
                    "<table border='1'><tr><th>Nome</th><th>Data</th><th>Nota</th><th>Detalhes</th></tr>";
            
            Connection conn = DriverManager.getConnection("jdbc:sqlite:jogos.db");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM jogos");
            
            while (rs.next()) {
                htmlContent += "<tr><td>" + rs.getString("nome") + "</td><td>" + rs.getString("data") + 
                				"</td><td>" + rs.getDouble("nota") + "</td><td>" + rs.getString("detalhes") + 
                																				"</td></tr>";
            }
            conn.close();
            
            htmlContent += "</table></body></html>";
            Files.write(Paths.get("jogos.html"), htmlContent.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        File file = new File("jogos.html");
        if (!file.exists()) {
            JOptionPane.showMessageDialog(null, "O arquivo jogos.html não foi encontrado!", "Erro", 
            																	JOptionPane.ERROR_MESSAGE);
        } else {
            int resposta = JOptionPane.showConfirmDialog(null, "Deseja abrir a planilha de jogos?", 
            												"Acessar Planilha", JOptionPane.YES_NO_OPTION);
            if (resposta == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().browse(file.toURI());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        JOptionPane.showMessageDialog(null, "Jogo: " + nomeJogo + "\nData de Lançamento: " + dataLancamento + 
        		"\n\n" + detalhesPlataformas.toString() + "\nNOTA FINAL: " + String.format("%.2f", notaFinal),
        		"Resumo das Reviews", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private static String formatarData(String data) {
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat formatoSaida = new SimpleDateFormat("dd/MM/yyyy");
            Date date = formatoEntrada.parse(data);
            return formatoSaida.format(date);
        } catch (ParseException e) {
            return "Data inválida";
        }
    }
}