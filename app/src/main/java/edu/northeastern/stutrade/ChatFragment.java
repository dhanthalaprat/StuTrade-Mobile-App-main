package edu.northeastern.stutrade;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.northeastern.stutrade.Models.ProductViewModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "username";
    private static final String ARG_PARAM2 = "email";
    private static final String ARG_PARAM3 = "selectedUserID";

    private String username, email;

    private RecyclerView rv_chat;
    private TextView tv_chat_with;
    private EditText et_message;
    private Button btn_send, btn_chat;
    private List<String> userList;
    private DatabaseReference chatToReference;
    private List<Chat> chatList;
    String userId, selectedUser, selectedUsername, selectedUserID;

    AutoCompleteTextView sortDropdown;
    TextInputLayout sortDropdownLayout;

    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param username username.
     * @param email email.
     * @return A new instance of fragment ChatFragment.
     */
    public static ChatFragment newInstance(String username, String email, String selectedUserID) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, username);
        args.putString(ARG_PARAM2, email);
        args.putString(ARG_PARAM3, selectedUserID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username = getArguments().getString(ARG_PARAM1);
            email = getArguments().getString(ARG_PARAM2);
            selectedUserID = getArguments().getString(ARG_PARAM3);
            userId = email.substring(0, email.indexOf("@"));
        }

        ProductViewModel productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        productViewModel.setCurrentFragment("chat_fragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize views
        et_message = view.findViewById(R.id.et_message);
        tv_chat_with = view.findViewById(R.id.tv_chat_with);
        btn_send = view.findViewById(R.id.btn_send);
        btn_chat = view.findViewById(R.id.btn_chat);
        sortDropdownLayout = view.findViewById(R.id.sortDropdownLayout);
        chatList = new ArrayList<>();

        // Initialize RecyclerView and its adapter
        rv_chat = view.findViewById(R.id.rv_chat);
        rv_chat.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv_chat.setAdapter(new ChatAdapter(chatList));

        getUserList(view);

        if (selectedUserID != null && !selectedUserID.isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(selectedUserID);
            userRef.addValueEventListener(new ValueEventListener() {
                 @Override
                 public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                     selectedUsername = dataSnapshot.child("name").getValue(String.class);
                     tv_chat_with.setText(selectedUsername);
                     displayUserChat(selectedUserID, view);
                 }
                 @Override
                 public void onCancelled(@NonNull DatabaseError error) {

                 }
             });

            rv_chat.setVisibility(View.VISIBLE);
            tv_chat_with.setVisibility(View.VISIBLE);
            et_message.setVisibility(View.VISIBLE);
            btn_send.setVisibility(View.VISIBLE);

            sortDropdown.setVisibility(View.GONE);
            sortDropdownLayout.setVisibility(View.GONE);
            btn_chat.setVisibility(View.GONE);
        }

        btn_chat.setOnClickListener(v -> {
            if (selectedUser != null) {
                rv_chat.setVisibility(View.VISIBLE);
                tv_chat_with.setVisibility(View.VISIBLE);
                et_message.setVisibility(View.VISIBLE);
                btn_send.setVisibility(View.VISIBLE);

                sortDropdown.setVisibility(View.GONE);
                sortDropdownLayout.setVisibility(View.GONE);
                btn_chat.setVisibility(View.GONE);

                selectedUsername = selectedUser.substring(0, selectedUser.indexOf("(")).trim();
                selectedUserID = selectedUser.substring(selectedUser.indexOf("(") + 1, selectedUser.indexOf(")")).trim();
                tv_chat_with.setText(selectedUsername);
                displayUserChat(selectedUserID, view);
            } else {
                Toast.makeText(getContext(), "Please select the user.", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle click events for the send button
        btn_send.setOnClickListener(v -> {
            String message = et_message.getText().toString().trim();
            if (!message.isEmpty()) {
                if (!selectedUserID.isEmpty()) {
                    // Initialize Firebase Database reference for chat
                    chatToReference = FirebaseDatabase.getInstance().getReference().child("chats")
                            .child(userId).child(selectedUserID).push();
                    Map<String, Object> messageToMap = new HashMap<>();
                    messageToMap.put("message", message);
                    messageToMap.put("isMessageSent", "true");
                    messageToMap.put("message_time", ServerValue.TIMESTAMP);
                    messageToMap.put("name", selectedUsername);
                    chatToReference.setValue(messageToMap);

                    DatabaseReference chatFromReference = FirebaseDatabase.getInstance().getReference().child("chats")
                            .child(selectedUserID).child(userId).push();
                    Map<String, Object> messageFromMap = new HashMap<>();
                    messageFromMap.put("message", message);
                    messageFromMap.put("isMessageSent", "false");
                    messageFromMap.put("message_time", ServerValue.TIMESTAMP);
                    messageFromMap.put("message_notified", "false");
                    messageFromMap.put("name", username);
                    chatFromReference.setValue(messageFromMap);

                    // clear the input field
                    et_message.setText("");
                }
            }
            else {
                Toast.makeText(getContext(), "Please type a message before sending.", Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    // get list of users from the database
    private void getUserList(View rootView) {
        // Initialize Firebase Database reference for users
        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference().child("users");

        // Populate the user list in the spinner
        userList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, userList);
        sortDropdown = rootView.findViewById(R.id.sortDropdown);
        sortDropdown.setAdapter(adapter);

        // Get the selected user
        sortDropdown.setOnItemClickListener((parent, view, position, id) -> selectedUser = userList.get(position));

        // Read users from the Firebase Database and update the spinner
        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.getKey();
                    String name = snapshot.child("name").getValue(String.class);
                    if (userId != null && !userId.equals(email.substring(0, email.indexOf("@")))) {
                        name = name + " (" + userId +")";
                        userList.add(name);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Error occurred, handle the error
                Toast.makeText(getContext(), "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // display the chat for two users
    private void displayUserChat(String selectedUserID, View view) {
        if (!selectedUserID.isEmpty()) {
            chatList.clear();
            // Initialize Firebase Database reference for chat
            chatToReference = FirebaseDatabase.getInstance().getReference().child("chats").child(userId).child(selectedUserID);

            chatToReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    chatList.clear();

                    if(dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            String isMessageSentString = snapshot.child("isMessageSent").getValue(String.class);
                            boolean isMessageSent = Boolean.parseBoolean(isMessageSentString);

                            String from_user;
                            if (isMessageSent) {
                                from_user = username;
                            } else {
                                from_user = selectedUsername;
                            }
                            String message = snapshot.child("message").getValue(String.class);

                            long timestamp = snapshot.child("message_time").getValue(Long.class);
                            String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
                            SimpleDateFormat formatter = new SimpleDateFormat(dateTimeFormat, Locale.US);
                            String message_time =  formatter.format(new Date(timestamp));

                                chatList.add(new Chat(message, from_user, message_time) {
                                @Override
                                public int compareTo(Chat chat) {
                                    return 0;
                                }
                            });
                        }

                        rv_chat = view.findViewById(R.id.rv_chat);
                        rv_chat.setLayoutManager(new LinearLayoutManager(getActivity()));
                        rv_chat.setAdapter(new ChatAdapter(chatList));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Error occurred, handle the error
                    Toast.makeText(getContext(), "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public boolean onBackPressed() {
        if (sortDropdown.getVisibility() == View.GONE) {
            sortDropdown.setVisibility(View.VISIBLE);
            sortDropdownLayout.setVisibility(View.VISIBLE);
            btn_chat.setVisibility(View.VISIBLE);

            rv_chat.setVisibility(View.GONE);
            tv_chat_with.setVisibility(View.GONE);
            et_message.setVisibility(View.GONE);
            btn_send.setVisibility(View.GONE);
            return true;
        }
        return false; // Let the activity handle the back button press
    }

    // Define a Chat class to hold chat data
    static abstract class Chat implements Comparable<Chat>{
        private final String message;
        private final String fromUsername;
        private final String messageTime;

        public Chat(String message, String fromUsername, String messageTime) {
            this.message = message;
            this.fromUsername = fromUsername;
            this.messageTime = messageTime;
        }

        public String getMessage() {
            return message;
        }

        public String fromUsername() {
            return fromUsername;
        }

        public String getMessageTime() {
            return messageTime;
        }
    }

    static class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {
        private final List<Chat> chats;

        public ChatAdapter(List<Chat> chats) {
            this.chats = chats;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChatViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            holder.bindThisData(chats.get(position));
        }

        @Override
        public int getItemCount() {
            return chats != null ? chats.size() : 0;
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView message;
        private final TextView from_username;
        private final TextView message_time;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            this.message = itemView.findViewById(R.id.tv_message);
            this.from_username = itemView.findViewById(R.id.tv_from_user);
            this.message_time = itemView.findViewById(R.id.tv_message_time);
        }

        public void bindThisData(Chat chatToBind) {
            message.setText(chatToBind.getMessage());
            from_username.setText(chatToBind.fromUsername());
            message_time.setText(chatToBind.getMessageTime());
        }
    }
}