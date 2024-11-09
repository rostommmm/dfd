package dev.excellent.client.screen.account.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.excellent.Excellent;
import dev.excellent.impl.util.file.AbstractFile;
import dev.excellent.impl.util.file.FileType;
import i.gishreloaded.protection.annotation.Native;
import lombok.NonNull;

import java.io.*;
import java.util.Map;

public class AccountFile extends AbstractFile {
    public AccountFile(File file) {
        super(file, FileType.ACCOUNT);
    }

    @Native
    @Override
    public boolean read() {
        if (!this.getFile().exists()) {
            return false;
        }

        try {

            final FileReader fileReader = new FileReader(this.getFile());
            final BufferedReader bufferedReader = new BufferedReader(fileReader);
            final JsonObject jsonObject = GSON.fromJson(bufferedReader, JsonObject.class);

            bufferedReader.close();
            fileReader.close();

            if (jsonObject == null) {
                return false;
            }
            int i = 0;
            for (Map.Entry<String, JsonElement> jsonElement : jsonObject.entrySet()) {
                i++;
                if (!jsonElement.getKey().equalsIgnoreCase("id-" + i))
                    continue;

                JsonObject accountJSONElement = jsonElement.getValue().getAsJsonObject();
                String name = accountJSONElement.get("username").getAsString();
                Account account = new Account(name);
                Excellent.getInst().getAccountManager().add(account);
            }

        } catch (final IOException ignored) {
            return false;
        }

        return true;
    }

    @Override
    public boolean write() {
        try {
            if (!this.getFile().exists()) {
                if (this.getFile().createNewFile()) {
                    System.out.println("Файл с списком макросов успешно создана.");
                } else {
                    System.out.println("Произошла ошибка при создании файла с списком макросов.");
                }
            }

            final JsonObject jsonObject = getJsonObject();

            final FileWriter fileWriter = new FileWriter(getFile());
            final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            GSON.toJson(jsonObject, bufferedWriter);

            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.flush();
            fileWriter.close();
        } catch (final IOException ignored) {
            return false;
        }

        return true;
    }

    @NonNull
    private static JsonObject getJsonObject() {
        final JsonObject jsonObject = new JsonObject();

        int i = 0;
        for (Account account : Excellent.getInst().getAccountManager()) {
            i++;
            final JsonObject accountJsonObject = new JsonObject();
            accountJsonObject.addProperty("username", account.getUsername());
            jsonObject.add("id-" + i, accountJsonObject);
        }
        return jsonObject;
    }
}