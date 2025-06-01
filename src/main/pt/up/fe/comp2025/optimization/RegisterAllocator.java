package pt.up.fe.comp2025.optimization;

import java.util.*;
import java.util.regex.*;

public class RegisterAllocator {

    public String optimize(String ollirCode) {
        System.out.println("Iniciando register allocation...");

        Pattern tmpPattern = Pattern.compile("(tmp\\d+)\\.i32");

        // Mapeamento de temporários antigos -> novos temporários reutilizados
        Map<String, String> allocation = new HashMap<>();

        // Temporários disponíveis para reutilização
        Deque<String> freeTemps = new ArrayDeque<>();

        // Contador para criar novos nomes de temporários
        int nextTmp = 0;

        // Contador de uso de cada temporário (simples)
        Map<String, Integer> usageCount = new HashMap<>();
        Matcher m = tmpPattern.matcher(ollirCode);
        while (m.find()) {
            String tmp = m.group(1);
            usageCount.put(tmp, usageCount.getOrDefault(tmp, 0) + 1);
        }

        StringBuilder newCode = new StringBuilder();

        for (String line : ollirCode.split("\\R")) {
            Matcher matcher = tmpPattern.matcher(line);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String originalTmp = matcher.group(1);
                String newTmp = allocation.get(originalTmp);

                if (newTmp == null) {
                    // Aloca novo ou reutiliza
                    newTmp = freeTemps.isEmpty() ? "tmp" + nextTmp++ : freeTemps.pop();
                    allocation.put(originalTmp, newTmp);
                }

                matcher.appendReplacement(sb, newTmp + ".i32");

                // Atualiza contador de uso
                int remaining = usageCount.compute(originalTmp, (k, v) -> v - 1);
                if (remaining == 0) {
                    freeTemps.push(newTmp);
                }
            }

            matcher.appendTail(sb);
            newCode.append(sb).append("\n");
        }

        System.out.println("Register allocation concluída.");
        return newCode.toString();
    }
}
