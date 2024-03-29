package com.example.demo.service;

import com.example.demo.config.BotConfig;
import com.example.demo.model.BotState;
import com.example.demo.model.UserInfo;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;


@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    final BotConfig botConfig;

    @Autowired
    public TelegramBot(UserProfileRepository userProfileRepository, UserRepository userRepository, BotConfig botConfig) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.botConfig = botConfig;

    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    //TODO: CREATE SERVICE FOLDER AND TRANSFER METHODS THERE
    @Override
    public void onUpdateReceived(Update update) {

        Long chatId = update.getMessage().getFrom().getId();
        UserInfo existingUser = userRepository.getByChatId(chatId);
        if (existingUser == null) {
            try {
                startMessage(update, chatId);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else if (existingUser.getBotState() == BotState.ASK_NAME) {
            askName(update,existingUser);
        }
        else if (existingUser.getBotState() == BotState.ASK_NUMBER) {
            askNum(update,existingUser);
        }
        else if (existingUser.getBotState() == BotState.ASK_SPECIALIZATION) {
            askSpec(update,existingUser);
        }
        else if (existingUser.getBotState() == BotState.ASK_DESCRIPTION) {
            askDesc(update,existingUser);
        }
        else if (existingUser.getBotState() == BotState.ASK_PROJECTS) {
            askProjects(update,existingUser);
        }
        else if (existingUser.getBotState() == BotState.ASK_SUM) {
            askSum(update,existingUser);
        }
        else if (existingUser.getBotState() == BotState.READY) {
            askProfile(update,existingUser);
        }
    }

    private void startMessage(Update update, Long chatId) throws TelegramApiException {
        UserInfo existingUser = userRepository.getByChatId(chatId);
        if (existingUser == null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUsername(update.getMessage().getFrom().getUserName());
            userInfo.setFirstName(update.getMessage().getFrom().getFirstName());
            userInfo.setLastName(update.getMessage().getFrom().getLastName() != null ? update.getMessage().getFrom().getLastName() : "null");
            userInfo.setChatId(chatId);
            userInfo.setBotState(BotState.ASK_NAME);
            userRepository.save(userInfo);
            SendMessage welcomeMessage = new SendMessage();
            welcomeMessage.setChatId(chatId);
            welcomeMessage.setText("Добро пожаловать на платформу Tulga.kz.");

            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(chatId);
            profileMessage.setText("Давайте заполним ваш профиль.\nВведите ваше имя.");

            try {
                execute(welcomeMessage);
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void askProfile(Update update,UserInfo userInfo){
        if (update.hasMessage()&&update.getMessage().hasText()){
            if(update.getMessage().getText().equals("2")){
                userInfo.setBotState(BotState.ASK_NAME);
                userRepository.save(userInfo);
                SendMessage profileMessage = new SendMessage();
                profileMessage.setChatId(update.getMessage().getChatId());
                profileMessage.setText("Давайте заполним ваш профиль.\nВведите ваше имя.");
                try {
                    execute(profileMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (update.getMessage().getText().equals("1")){
                SendMessage profileMessage = new SendMessage();
                profileMessage.setChatId(update.getMessage().getChatId());
                UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
                String profileInfo =
                        "\nИмя: " + existingProfile.getName() +
                        "\nНомер телефона: " + existingProfile.getNumber() +
                        "\nСпециализация: " + existingProfile.getSpecialization() +
                        "\nОписание: " + existingProfile.getDescription() +
                        "\nПроекты: " + existingProfile.getProjects() +
                        "\nДеньги: " + existingProfile.getMoney();

                profileMessage.setText(profileInfo);

                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                replyKeyboardMarkup.setResizeKeyboard(true);
                replyKeyboardMarkup.setOneTimeKeyboard(true);
                List<KeyboardRow> keyboardRowList = new ArrayList<>();
                KeyboardRow keyboardRow = new KeyboardRow();
                keyboardRow.add(new KeyboardButton("1"));
                keyboardRow.add(new KeyboardButton("2"));
                keyboardRowList.add(keyboardRow);
                replyKeyboardMarkup.setKeyboard(keyboardRowList);
                profileMessage.setReplyMarkup(replyKeyboardMarkup);

                SendMessage msg = new SendMessage();
                msg.setChatId(update.getMessage().getChatId());
                msg.setText("""
                        Ожидайте пока с вами не свяжутся.
                        
                        1. Смотреть анкету.
                        2. Заполнить анкету заново.
                        """);

                try {
                    execute(profileMessage);
                    execute(msg);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            }

        else{
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Неправильный ввод, пожалуйств введите заново");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void askSum(Update update,UserInfo userInfo){
        if (update.hasMessage()&&update.getMessage().hasText()){
            UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
            existingProfile.setMoney(update.getMessage().getText());
            userProfileRepository.save(existingProfile);
            userInfo.setBotState(BotState.READY);
            userRepository.save(userInfo);
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("1"));
            keyboardRow.add(new KeyboardButton("2"));
            keyboardRowList.add(keyboardRow);
            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            profileMessage.setReplyMarkup(replyKeyboardMarkup);

            String profileInfo =
                    "\nИмя: " + existingProfile.getName() +
                    "\nНомер телефона: " + existingProfile.getNumber() +
                    "\nСпециализация: " + existingProfile.getSpecialization() +
                    "\nОписание: " + existingProfile.getDescription() +
                    "\nПроекты: " + existingProfile.getProjects() +
                    "\nДеньги: " + existingProfile.getMoney();

            profileMessage.setText(profileInfo);

            SendMessage msg = new SendMessage();
            msg.setChatId(update.getMessage().getChatId());
            msg.setText("""
                    Вы успешно создали анкету. Ожидайте пока с вами не свяжутся.
                    
                    1. Смотреть анкету.
                    2. Заполнить анкету заново.
                    """);

            try {
                execute(profileMessage);
                execute(msg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Неправильный ввод, пожалуйств введите заново");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void askProjects(Update update,UserInfo userInfo){
        if (update.hasMessage()&&update.getMessage().hasText()){
            UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
            existingProfile.setProjects(update.getMessage().getText());
            userProfileRepository.save(existingProfile);
            userInfo.setBotState(BotState.ASK_SUM);
            userRepository.save(userInfo);
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Напишите ценовую политику за каждую вашу услугу." + "\n" + "Например: Настроить таргет - 50.000 тенге ");

            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Неправильный ввод, пожалуйств введите заново");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void askDesc(Update update,UserInfo userInfo){
        if (update.hasMessage()&&update.getMessage().hasText()) {
            UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
            existingProfile.setDescription(update.getMessage().getText());
            userProfileRepository.save(existingProfile);
            userInfo.setBotState(BotState.ASK_PROJECTS);
            userRepository.save(userInfo);
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Прикрепите ссылки на ваши проекты, если нет проектов, нажмите пропустить. [Файлы не принимаются]");
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Пропустить"));
            keyboardRowList.add(keyboardRow);
            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            profileMessage.setReplyMarkup(replyKeyboardMarkup);

            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Неправильный ввод, пожалуйств введите заново");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void askSpec(Update update,UserInfo userInfo){
        if (update.hasMessage()&&update.getMessage().hasText()) {
            UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
            existingProfile.setSpecialization(update.getMessage().getText());
            userProfileRepository.save(existingProfile);
            userInfo.setBotState(BotState.ASK_DESCRIPTION);
            userRepository.save(userInfo);
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Расскажите о себе \n Это самое первое, что видят клиенты, поэтому учитывайте это. Выделяйтесь, описывая свой опыт своими словами.");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Неправильный ввод, пожалуйств введите заново");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void askNum(Update update,UserInfo userInfo){
        if (update.hasMessage()&&update.getMessage().hasText()){
            UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
            existingProfile.setNumber(update.getMessage().getText());
            userProfileRepository.save(existingProfile);
            userInfo.setBotState(BotState.ASK_SPECIALIZATION);
            userRepository.save(userInfo);
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Выберите вашу специальность.");
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Смм"));
            keyboardRow.add(new KeyboardButton("Дизайнер"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Таргетолог"));
            keyboardRow.add(new KeyboardButton("Мобилограф"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("It специалист"));
            keyboardRow.add(new KeyboardButton("Монтажер"));
            keyboardRowList.add(keyboardRow);

            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            profileMessage.setReplyMarkup(replyKeyboardMarkup);


            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else if(update.getMessage().hasContact()){
            UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
            String num = update.getMessage().getContact().getPhoneNumber();
            existingProfile.setNumber(num);
            userProfileRepository.save(existingProfile);
            userInfo.setBotState(BotState.ASK_SPECIALIZATION);
            userRepository.save(userInfo);
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Выберите вашу специальность.");
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Смм"));
            keyboardRow.add(new KeyboardButton("Дизайнер"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Таргетолог"));
            keyboardRow.add(new KeyboardButton("Мобилограф"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("It специалист"));
            keyboardRow.add(new KeyboardButton("Монтажер"));
            keyboardRowList.add(keyboardRow);

            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            profileMessage.setReplyMarkup(replyKeyboardMarkup);


            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Неправильный ввод, пожалуйств введите заново");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void askName(Update update,UserInfo userInfo) {
        if (update.hasMessage()&&update.getMessage().hasText()){
            UserProfile existingProfile = userProfileRepository.findByUserInfo(userInfo);
            if (existingProfile == null) {
                UserProfile userProfile = new UserProfile();
                userProfile.setName(update.getMessage().getText());
                userProfile.setUserInfo(userInfo);
                userInfo.setBotState(BotState.ASK_NUMBER);
                userRepository.save(userInfo);
                userProfileRepository.save(userProfile);


                SendMessage profileMessage = new SendMessage();
                profileMessage.setChatId(update.getMessage().getChatId());
                profileMessage.setText("Введите ваш номер телефона.");

                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                replyKeyboardMarkup.setResizeKeyboard(true);
                replyKeyboardMarkup.setOneTimeKeyboard(true);
                List<KeyboardRow> keyboardRowList = new ArrayList<>();
                KeyboardRow keyboardRow = new KeyboardRow();
                KeyboardButton keyboardButton = new KeyboardButton();
                keyboardButton.setText("Отправить номер");
                keyboardButton.setRequestContact(true);
                keyboardRow.add(keyboardButton);
                keyboardRowList.add(keyboardRow);
                replyKeyboardMarkup.setKeyboard(keyboardRowList);
                profileMessage.setReplyMarkup(replyKeyboardMarkup);
                try {
                    execute(profileMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

            } else {

                existingProfile.setName(update.getMessage().getText());
                userProfileRepository.save(existingProfile);
                userInfo.setBotState(BotState.ASK_NUMBER);
                userRepository.save(userInfo);
                SendMessage profileMessage = new SendMessage();
                profileMessage.setChatId(update.getMessage().getChatId());
                profileMessage.setText("Введите ваш номер телефона.");
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                replyKeyboardMarkup.setResizeKeyboard(true);
                replyKeyboardMarkup.setOneTimeKeyboard(true);
                List<KeyboardRow> keyboardRowList = new ArrayList<>();
                KeyboardRow keyboardRow = new KeyboardRow();
                KeyboardButton keyboardButton = new KeyboardButton();
                keyboardButton.setText("Отправить номер");
                keyboardButton.setRequestContact(true);
                keyboardRow.add(keyboardButton);
                keyboardRowList.add(keyboardRow);
                replyKeyboardMarkup.setKeyboard(keyboardRowList);
                profileMessage.setReplyMarkup(replyKeyboardMarkup);
                try {
                    execute(profileMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        else{
            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(update.getMessage().getChatId());
            profileMessage.setText("Неправильный ввод, пожалуйств введите заново");
            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

    }


}


