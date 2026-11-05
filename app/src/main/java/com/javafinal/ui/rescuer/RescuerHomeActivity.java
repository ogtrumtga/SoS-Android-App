package com.javafinal.ui.rescuer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.javafinal.R;
import com.javafinal.data.MockDataRepository;
import com.javafinal.model.SosRequest;
import com.javafinal.util.NavigationHelper;

import java.util.List;

public class RescuerHomeActivity extends AppCompatActivity {

    private SosRequestAdapter adapter;
    private LinearLayout layoutActiveMission;
    private TextView tvActiveMissionTitle;
    private MaterialButton btnOpenActiveMission;

    private TextView tvWaitingCount;
    private TextView tvHighPriorityCount;
    private TextView tvScanRadius;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rescuer_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MockDataRepository repo = MockDataRepository.getInstance();
        repo.setCurrentRole(MockDataRepository.ROLE_RESCUER);

        layoutActiveMission = findViewById(R.id.layoutActiveMission);
        tvActiveMissionTitle = findViewById(R.id.tvActiveMissionTitle);
        btnOpenActiveMission = findViewById(R.id.btnOpenActiveMission);

        MaterialButton btnEmergencyAlert = findViewById(R.id.btnEmergencyAlert);
        MaterialButton btnProfile = findViewById(R.id.btnProfile);

        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvHighPriorityCount = findViewById(R.id.tvHighPriorityCount);
        tvScanRadius = findViewById(R.id.tvScanRadius);

        ChipGroup chipGroupFilter = findViewById(R.id.chipGroupRescuerFilter);
        RecyclerView rvSosRequests = findViewById(R.id.rvSosRequests);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        rvSosRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SosRequestAdapter(repo.getRescueQueue(currentFilter),
                new SosRequestAdapter.OnSosActionListener() {
                    @Override
                    public void onViewDetail(SosRequest request) {
                        repo.setLastViewedSos(request);
                        Intent intent = new Intent(RescuerHomeActivity.this, SosDetailActivity.class);
                        intent.putExtra("SOS_ID", request.getId());
                        startActivity(intent);
                    }

                    @Override
                    public void onAcceptMission(SosRequest request) {
                        handleAcceptMission(request);
                    }
                });
        rvSosRequests.setAdapter(adapter);

        btnOpenActiveMission.setOnClickListener(v -> {
            SosRequest active = repo.getMyActiveRescuerMission();
            if (active != null) {
                repo.setLastViewedSos(active);
                Intent intent = new Intent(RescuerHomeActivity.this, RealtimeTrackingActivity.class);
                intent.putExtra("SOS_ID", active.getId());
                startActivity(intent);
            }
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipUrgent) {
                currentFilter = SosRequest.TYPE_NGAP_LUT;
            } else if (id == R.id.chipNearMe) {
                currentFilter = SosRequest.TYPE_Y_TE;
            } else if (id == R.id.chipPrivate) {
                currentFilter = "PRIVATE";
            } else if (id == R.id.chipPublic) {
                currentFilter = "PUBLIC";
            } else {
                currentFilter = "ALL";
            }
            refreshQueueAndStats();
        });

        btnEmergencyAlert.setOnClickListener(v -> NavigationHelper.showNotificationsDialog(this));
        btnProfile.setOnClickListener(v ->
                NavigationHelper.showProfileDialog(this, this::refreshQueueAndStats));

        bottomNav.setSelectedItemId(R.id.nav_rescuer_requests);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_rescuer_requests) {
                return true;
            } else if (itemId == R.id.nav_rescuer_map || itemId == R.id.nav_rescuer_missions) {
                SosRequest active = repo.getMyActiveRescuerMission();
                if (active != null) {
                    Intent intent = new Intent(RescuerHomeActivity.this, RealtimeTrackingActivity.class);
                    intent.putExtra("SOS_ID", active.getId());
                    startActivity(intent);
                } else {
                    NavigationHelper.showSnack(this, "Chưa có ca cứu hộ nào đang xử lý");
                }
                return false;
            } else if (itemId == R.id.nav_rescuer_team) {
                NavigationHelper.showRescueTeamSheet(this, this::refreshQueueAndStats);
                return false;
            }
            return false;
        });

        refreshQueueAndStats();
    }

    private void handleAcceptMission(SosRequest request) {
        MockDataRepository repo = MockDataRepository.getInstance();
        SosRequest busyMission = repo.getMyActiveRescuerMission();

        if (busyMission != null && !busyMission.getId().equals(request.getId())) {
            NavigationHelper.showConfirmSheet(
                    this,
                    null,
                    "Đội đang xử lý ca khác",
                    busyMission.getEmergencyTypeLabel() + " • " + busyMission.getStatusLabel(),
                    "Mở ca hiện tại",
                    false,
                    () -> {
                        repo.setLastViewedSos(busyMission);
                        Intent intent = new Intent(RescuerHomeActivity.this, RealtimeTrackingActivity.class);
                        intent.putExtra("SOS_ID", busyMission.getId());
                        startActivity(intent);
                    },
                    "Đóng",
                    null
            );
            return;
        }

        repo.acceptSosMission(request);
        Intent intent = new Intent(RescuerHomeActivity.this, RealtimeTrackingActivity.class);
        intent.putExtra("SOS_ID", request.getId());
        startActivity(intent);
    }

    private void refreshQueueAndStats() {
        MockDataRepository repo = MockDataRepository.getInstance();

        SosRequest active = repo.getMyActiveRescuerMission();
        if (active != null) {
            layoutActiveMission.setVisibility(View.VISIBLE);
            tvActiveMissionTitle.setText("● " + active.getEmergencyTypeLabel() + " • " + active.getStatusLabel());
        } else {
            layoutActiveMission.setVisibility(View.GONE);
        }

        List<SosRequest> filtered = repo.getRescueQueue(currentFilter);
        adapter.submitList(filtered);

        List<SosRequest> allWaiting = repo.getRescueQueue("ALL");
        int pubCount = repo.getRescueQueue("PUBLIC").size();
        int privCount = repo.getRescueQueue("PRIVATE").size();

        tvWaitingCount.setText(String.valueOf(allWaiting.size()));
        tvHighPriorityCount.setText(String.valueOf(pubCount));
        tvScanRadius.setText(String.valueOf(privCount));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshQueueAndStats();
    }
}
