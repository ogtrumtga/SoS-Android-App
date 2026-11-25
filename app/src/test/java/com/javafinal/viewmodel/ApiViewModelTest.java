package com.javafinal.viewmodel;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javafinal.model.SosApiModels;
import com.javafinal.model.RescueTeam;
import com.javafinal.model.TeamApiModels;
import com.javafinal.model.SosRequest;
import com.javafinal.model.User;
import com.javafinal.network.ApiClient;
import com.javafinal.network.ApiError;
import com.javafinal.network.RealtimeProtocol;
import com.javafinal.network.SessionStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Kiểm tra request thật qua OkHttp interceptor; không cần backend hoặc Android device. */
public class ApiViewModelTest {
    private static final String USER_JSON = "{\"id\":\"u1\",\"full_name\":\"An\","
            + "\"phone\":\"0901234567\",\"role\":\"RESCUER\",\"is_valid\":true}";
    private final Queue<Runnable> callbacks = new ConcurrentLinkedQueue<>();
    private final Queue<Request> requests = new ConcurrentLinkedQueue<>();
    private final List<BaseApiViewModel> viewModels = new ArrayList<>();
    private volatile String responseBody;
    private volatile int responseCode;
    private volatile boolean networkFailure;
    private SessionStore session;
    private ApiClient api;
    private OkHttpClient http;
    private AuthViewModel auth;
    private UserViewModel users;
    private SosViewModel sos;
    private NotificationViewModel notifications;
    private RescuerViewModel rescuers;
    private TeamViewModel teams;
    private LocationViewModel locations;
    private EmergencyTypeViewModel types;
    private SystemViewModel system;

    @Before public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { callbacks.add(runnable); }
            @Override public boolean isMainThread() { return true; }
        });
        responseCode = 200;
        responseBody = "{\"success\":true,\"access_token\":\"issued-token\",\"user\":" + USER_JSON + "}";
        session = new SessionStore();
        session.save("initial-token", new Gson().fromJson(USER_JSON, User.class));
        http = new OkHttpClient.Builder().retryOnConnectionFailure(false).addInterceptor(chain -> {
            requests.add(chain.request());
            if (networkFailure) throw new IOException("test network failure");
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(responseCode).message("test").header("Retry-After", "25")
                    .body(ResponseBody.create(responseBody, MediaType.get("application/json"))).build();
        }).build();
        api = new ApiClient("http://localhost:8000/", session, callbacks::add, http, new Gson());
        auth = keep(new AuthViewModel(api));
        users = keep(new UserViewModel(api));
        sos = keep(new SosViewModel(api));
        notifications = keep(new NotificationViewModel(api));
        rescuers = keep(new RescuerViewModel(api));
        teams = keep(new TeamViewModel(api));
        locations = keep(new LocationViewModel(api));
        types = keep(new EmergencyTypeViewModel(api));
        system = keep(new SystemViewModel(api));
    }

    private <T extends BaseApiViewModel> T keep(T vm) { viewModels.add(vm); return vm; }

    @After public void tearDown() {
        for (BaseApiViewModel vm : viewModels) vm.onCleared();
        http.dispatcher().executorService().shutdownNow();
        http.connectionPool().evictAll();
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test public void all32HttpRoutesMatchSpecificationIncludingCompatibilityRoute() throws Exception {
        endpoint("POST", "/api/auth/request-otp", false, "{\"phone\":\"0901234567\",\"role\":\"USER\"}",
                () -> auth.requestOtp(" 0901234567 ", "USER"));
        endpoint("POST", "/api/auth/verify-otp", false, "{\"phone\":\"0901234567\",\"otp\":\"123456\"}",
                () -> auth.verifyOtp("0901234567", "123456"));
        assertEquals("issued-token", session.getAccessToken());
        assertTrue(session.getUser().isRescuer());
        endpoint("GET", "/api/auth/me", true, null, auth::loadMe);
        endpoint("POST", "/api/auth/dev-link-telegram", false,
                "{\"phone\":\"0901234567\",\"telegram_chat_id\":123456789}",
                () -> auth.devLinkTelegram("0901234567", 123456789L));
        endpoint("GET", "/api/users/profile", true, null, users::loadProfile);
        endpoint("PUT", "/api/users/full-name", true, "{\"full_name\":\"An\"}", () -> users.updateFullName(" An "));
        endpoint("PUT", "/api/users/profile", true, "{\"full_name\":\"An\"}", () -> users.updateProfile("An"));
        endpoint("POST", "/api/sos", true,
                "{\"emergency_type\":\"NGAP_LUT\",\"description\":\"Cần hỗ trợ\",\"visibility\":\"PUBLIC\",\"initial_lat\":10.7,\"initial_lng\":106.7}",
                () -> sos.createSos(new SosApiModels.CreateSosRequest("NGAP_LUT", "Cần hỗ trợ", "PUBLIC", 10.7, 106.7)));
        endpoint("GET", "/api/sos/my-active", true, null, sos::loadMyActive);
        endpoint("GET", "/api/sos/public/map", false, null, sos::loadPublicMap);
        endpoint("GET", "/api/sos/s1", true, null, () -> sos.loadDetail("s1"));
        endpoint("PUT", "/api/sos/s1/visibility", true, "{\"visibility\":\"PRIVATE\"}",
                () -> sos.updateVisibility("s1", "PRIVATE"));
        endpoint("POST", "/api/sos/s1/cancel", true, "{}", () -> sos.cancelSos("s1"));
        endpoint("GET", "/api/sos/s1/logs", true, null, () -> sos.loadLogs("s1"));
        endpoint("GET", "/api/sos/notifications/my", true, null, notifications::loadMyNotifications);
        endpoint("PUT", "/api/sos/notifications/n1/read", true, "{}", () -> notifications.markAsRead("n1"));
        endpoint("GET", "/api/rescuer/queue", true, null, rescuers::loadQueue);
        endpoint("GET", "/api/rescuer/my-active", true, null, rescuers::loadMyActive);
        endpoint("GET", "/api/rescuer/missions", true, null, rescuers::loadMyMissions);
        endpoint("POST", "/api/rescuer/sos/s1/accept", true, "{}", () -> rescuers.acceptSos("s1"));
        endpoint("POST", "/api/rescuer/sos/s1/status", true, "{\"status\":\"RESCUING\"}",
                () -> rescuers.updateMissionStatus("s1", "RESCUING"));
        endpoint("POST", "/api/rescuer/sos/s1/release", true, "{}", () -> rescuers.releaseSos("s1"));
        endpoint("POST", "/api/rescuer/sos/s1/complete", true, "{}", () -> rescuers.completeSos("s1"));
        endpoint("GET", "/api/rescuer/teams", true, null, rescuers::loadTeams);
        endpoint("GET", "/api/rescuer/stats?period=DAY&date=2026-11-25", true, null,
                () -> rescuers.loadStats("DAY", "2026-11-25", null, null));
        endpoint("POST", "/api/location/s1", true,
                "{\"lat\":10.7,\"lng\":106.7,\"accuracy_m\":4.5,\"save_history\":true}",
                () -> locations.updateLocation("s1", 10.7, 106.7, 4.5, true));
        endpoint("GET", "/api/location/s1/current", true, null, () -> locations.loadCurrent("s1"));
        endpoint("GET", "/api/location/s1/trail?actor=RESCUER&limit=500", true, null,
                () -> locations.loadTrail("s1", "RESCUER", 500));
        endpoint("GET", "/api/emergency-types", false, null, types::loadAll);
        endpoint("GET", "/api/emergency-types/e1", false, null, () -> types.loadDetail("e1"));
        endpoint("POST", "/api/emergency-types", true, "{\"name\":\"Y_TE\",\"description\":\"Y tế\"}",
                () -> types.create(" Y_TE ", "Y tế"));
        endpoint("PUT", "/api/emergency-types/e1", true, "{\"name\":\"Y_TE\",\"description\":\"Y tế\"}",
                () -> types.update("e1", "Y_TE", "Y tế"));
        endpoint("DELETE", "/api/emergency-types/e1", true, null, () -> types.delete("e1"));
        endpoint("GET", "/health", false, null, system::checkHealth);
    }

    @Test public void inactiveSosAndNestedQueueAreDecodedCorrectly() throws Exception {
        responseBody = "{\"active\":false,\"sos\":null}";
        ApiState<SosApiModels.ActiveSosResponse> inactive = await(sos.loadMyActive());
        assertEquals(ApiState.Status.SUCCESS, inactive.getStatus());
        assertFalse(inactive.getData().isActive());
        assertNull(inactive.getData().getSos());
        responseBody = "{\"success\":true,\"data\":[{\"sos_id\":\"s1\",\"requester\":{\"full_name\":\"An\",\"phone\":\"0901234567\"}}]}";
        SosRequest item = await(rescuers.loadQueue()).getData().getData().get(0);
        assertEquals("s1", item.getId());
        assertEquals("An", item.getRequesterName());
        assertEquals("0901234567", item.getRequesterPhone());
        responseBody = "{\"active\":true,\"case\":{\"sos_id\":\"s2\",\"emergency_type_id\":\"eeeeeeee-0006-0000-0000-000000000006\",\"emergency_type\":\"Y_TE_KHAN_CAP\",\"victim\":{\"id\":\"u2\",\"full_name\":\"Bình\",\"phone\":\"0909998888\"}}}";
        SosRequest activeCase = await(rescuers.loadMyActive()).getData().getSos();
        assertEquals("s2", activeCase.getId());
        assertEquals("Bình", activeCase.getRequesterName());
        assertEquals("0909998888", activeCase.getRequesterPhone());
        responseBody = "{\"success\":true,\"count\":2,\"data\":[{\"id\":\"eeeeeeee-0006-0000-0000-000000000006\",\"name\":\"Y_TE_KHAN_CAP\",\"description\":\"Cấp cứu y tế khẩn cấp, bệnh nhân nguy kịch cần hỗ trợ y bác sĩ\"},{\"id\":\"custom-uuid\",\"name\":\"CUU_HO_THUY\",\"description\":\"Cứu hộ đường thủy, lật thuyền trên sông\"}]}";
        await(types.loadAll());
        assertEquals("Cấp cứu y tế khẩn cấp", activeCase.getEmergencyTypeLabel());
        SosRequest customSos = new SosRequest();
        customSos.setEmergencyTypeId("custom-uuid");
        assertEquals("Cứu hộ đường thủy", customSos.getEmergencyTypeLabel());
    }

    @Test public void rescuerStatsAreDecodedCorrectly() throws Exception {
        responseBody = "{\"success\":true,\"period\":\"MONTH\",\"date\":null,\"month\":\"2026-11\","
                + "\"summary\":{\"rescued_count\":3,\"distance_km\":5.75,\"tracked_missions\":2},"
                + "\"overall\":{\"rescued_count\":3,\"distance_km\":5.75,\"tracked_missions\":2},"
                + "\"by_emergency_type\":[{\"emergency_type_id\":\"e1\",\"emergency_type\":\"NGAP_LUT\","
                + "\"description\":\"Ngập lụt, triều cường\",\"rescued_count\":2,\"distance_km\":4.25,\"tracked_missions\":2},"
                + "{\"emergency_type_id\":\"e2\",\"emergency_type\":\"TAI_NAN\","
                + "\"description\":\"Tai nạn giao thông\",\"rescued_count\":1,\"distance_km\":1.5,\"tracked_missions\":0}]}";
        ApiState<SosApiModels.RescuerStatsResponse> state =
                await(rescuers.loadStats("MONTH", null, "2026-11", null));
        assertEquals(ApiState.Status.SUCCESS, state.getStatus());
        SosApiModels.RescuerStatsResponse stats = state.getData();
        assertEquals("MONTH", stats.getPeriod());
        assertEquals(3, stats.getOverall().getRescuedCount());
        assertEquals(5.75, stats.getOverall().getDistanceKm(), 0.001);
        assertEquals(2, stats.getByEmergencyType().size());
        assertEquals("Ngập lụt", stats.getByEmergencyType().get(0).getDisplayLabel());
        assertEquals(2, stats.getByEmergencyType().get(0).getRescuedCount());
    }

    @Test public void invalidInputsNeverReachNetwork() {
        invalid(auth.requestOtp("abc", "USER"));
        invalid(auth.requestOtp("0901234567", "ADMIN"));
        invalid(auth.verifyOtp("0901234567", "12a456"));
        invalid(auth.devLinkTelegram("0901234567", 0));
        invalid(users.updateFullName(" "));
        invalid(sos.loadDetail(" .. "));
        invalid(sos.createSos(null));
        invalid(sos.createSos(new SosApiModels.CreateSosRequest("Y_TE", "", "PUBLIC", Double.NaN, 106)));
        invalid(sos.updateVisibility("s1", "INVALID"));
        invalid(notifications.markAsRead(""));
        invalid(rescuers.updateMissionStatus("s1", "COMPLETED"));
        invalid(rescuers.loadStats("WEEK", null, null, null));
        invalid(locations.updateLocation("s1", 91, 106, 5, true));
        invalid(locations.updateLocation("s1", 10, 106, -1, true));
        invalid(locations.loadTrail("s1", "USER", 100));
        invalid(locations.loadTrail("s1", null, 501));
        invalid(types.create("", null));
        invalid(types.update(null, "Y_TE", null));
        assertTrue(requests.isEmpty());
    }

    @Test public void noTokenBlocksPrivateCallsButAllowsPublicMap() throws Exception {
        session.clear();
        assertEquals(401, sos.loadMyActive().getValue().getError().getStatusCode());
        assertTrue(requests.isEmpty());
        endpoint("GET", "/api/sos/public/map", false, null, sos::loadPublicMap);
    }

    @Test public void handlesFastApiErrorsRateLimitsAndConflictWithoutRetry() throws Exception {
        responseCode = 422;
        responseBody = "{\"detail\":[{\"loc\":[\"body\",\"lat\"],\"msg\":\"Latitude invalid\",\"type\":\"value_error\"}]}";
        ApiError validation = await(system.checkHealth()).getError();
        assertEquals(422, validation.getStatusCode());
        assertEquals("Latitude invalid", validation.getMessage());
        responseCode = 429;
        responseBody = "{\"detail\":\"Vui lòng đợi 25 giây\"}";
        ApiError limited = await(auth.requestOtp("0901234567", "USER")).getError();
        assertEquals("25", limited.getRetryAfter());
        responseCode = 409;
        responseBody = "{\"detail\":{\"message\":\"Bạn đang có yêu cầu SOS chưa kết thúc.\",\"active_sos_id\":\"s1\",\"status\":\"WAITING\"}}";
        ApiError conflict = await(rescuers.acceptSos("s1")).getError();
        assertTrue(conflict.isConflict());
        assertEquals("Bạn đang có yêu cầu SOS chưa kết thúc.", conflict.getMessage());
        assertEquals(3, requests.size());
    }

    @Test public void unauthorizedResponseClearsOnlyTheSessionThatSentIt() throws Exception {
        responseCode = 401;
        responseBody = "{\"detail\":\"Token expired\"}";
        assertTrue(await(auth.loadMe()).getError().isUnauthorized());
        assertFalse(session.isLoggedIn());
        session.save("old-token", new User());
        LiveData<ApiState<com.javafinal.model.AuthModels.UserResponse>> pending = auth.loadMe();
        waitForCallback();
        session.save("new-token", new User());
        assertTrue(await(pending).getError().isUnauthorized());
        assertEquals("new-token", session.getAccessToken());
    }

    @Test public void staleSuccessfulProfileDoesNotOverwriteNewSession() throws Exception {
        LiveData<ApiState<com.javafinal.model.AuthModels.UserResponse>> pending = users.loadProfile();
        waitForCallback();
        User replacement = new User();
        replacement.setFullName("New session");
        session.save("new-token", replacement);
        assertEquals(ApiState.Status.ERROR, await(pending).getStatus());
        assertEquals("New session", session.getUser().getFullName());
    }

    @Test public void networkMalformedJsonAndBusinessFailureBecomeErrors() throws Exception {
        networkFailure = true;
        assertEquals(ApiError.Kind.NETWORK, await(system.checkHealth()).getError().getKind());
        networkFailure = false;
        responseBody = "<html>gateway error</html>";
        assertEquals(ApiError.Kind.PARSE, await(system.checkHealth()).getError().getKind());
        responseBody = "{\"success\":false,\"message\":\"Không thể thực hiện\"}";
        ApiError business = await(system.checkHealth()).getError();
        assertEquals(ApiError.Kind.BUSINESS, business.getKind());
        assertEquals("Không thể thực hiện", business.getMessage());
        responseBody = "null";
        assertEquals(ApiError.Kind.PARSE, await(system.checkHealth()).getError().getKind());
        responseCode = 422;
        responseBody = "{\"detail\":[{\"msg\":null}]}";
        assertEquals(ApiError.Kind.HTTP, await(system.checkHealth()).getError().getKind());
    }

    @Test public void emptyDeleteResponseSucceeds() throws Exception {
        responseCode = 204;
        responseBody = "";
        assertEquals(ApiState.Status.SUCCESS, await(types.delete("e1")).getStatus());
    }

    @Test public void clearingViewModelCancelsQueuedResponse() throws Exception {
        LiveData<ApiState<JsonObject>> pending = system.checkHealth();
        waitForCallback();
        system.onCleared();
        drainCallbacks();
        assertEquals(ApiError.Kind.CANCELLED, pending.getValue().getError().getKind());
    }

    @Test public void logoutCancelsPendingOtpVerification() throws Exception {
        LiveData<?> pending = auth.verifyOtp("0901234567", "123456");
        waitForCallback();
        auth.logout();
        drainCallbacks();
        assertFalse(session.isLoggedIn());
        assertEquals(ApiError.Kind.CANCELLED, ((ApiState<?>) pending.getValue()).getError().getKind());
    }

    @Test public void sosStateMachineRejectsSkippingAndTerminalActions() {
        assertTrue(SosRules.canTransition("ACCEPTED", "RESCUING"));
        assertTrue(SosRules.canTransition("RESCUING", "ARRIVED"));
        assertFalse(SosRules.canTransition("ACCEPTED", "ARRIVED"));
        assertTrue(SosRules.canRelease("RESCUING"));
        assertFalse(SosRules.canRelease("ARRIVED"));
        assertTrue(SosRules.canComplete("ARRIVED"));
        assertFalse(SosRules.canComplete("RESCUING"));
        assertTrue(SosRules.isTerminal("CANCELLED"));
        SosRequest mission = new SosRequest();
        mission.setId("s1");
        mission.setStatus("ACCEPTED");
        invalid(rescuers.updateMissionStatus(mission, "ARRIVED"));
        assertTrue(requests.isEmpty());
    }

    @Test public void websocketUsesTypeOnWireAndAcceptsBothIncomingKeys() {
        assertEquals("{\"type\":\"PING\"}", RealtimeProtocol.ping());
        assertEquals("SUBSCRIBE_SOS", JsonParser.parseString(RealtimeProtocol.subscribe("s1"))
                .getAsJsonObject().get("type").getAsString());
        assertEquals("PONG", RealtimeProtocol.decode(new Gson(), "{\"type\":\"PONG\"}").getEvent());
        assertEquals("SOS_ACCEPTED", RealtimeProtocol.decode(new Gson(),
                "{\"event\":\"SOS_ACCEPTED\",\"sos_id\":\"s1\"}").getEvent());
    }

    @Test public void factoryCreatesEveryViewModelAndRealtimeNeedsAuthentication() {
        ApiViewModelFactory factory = new ApiViewModelFactory(api);
        Class<?>[] classes = {AuthViewModel.class, UserViewModel.class, SosViewModel.class,
                NotificationViewModel.class, RescuerViewModel.class, TeamViewModel.class, LocationViewModel.class,
                EmergencyTypeViewModel.class, SystemViewModel.class};
        for (Class<?> type : classes) assertNotNull(factory.create(type.asSubclass(BaseApiViewModel.class)));
        session.clear();
        RealtimeViewModel realtime = keep(factory.create(RealtimeViewModel.class));
        realtime.connect();
        assertEquals(RealtimeViewModel.ConnectionState.ERROR, realtime.getConnectionState().getValue());
        assertEquals(401, realtime.getErrors().getValue().getStatusCode());
        ViewModelStore store = new ViewModelStore();
        store.put("realtime", realtime);
        store.clear();
    }

    @Test public void rescuerMapAuthenticatesAndPreservesPublicAndPrivateExactLocations() throws Exception {
        responseBody = "{\"success\":true,\"count\":2,\"data\":["
                + "{\"sos_id\":\"s1\",\"emergency_type\":\"NGAP_LUT\",\"visibility\":\"PUBLIC\",\"status\":\"WAITING\","
                + "\"initial_lat\":10.781234,\"initial_lng\":106.691234},"
                + "{\"sos_id\":\"s2\",\"emergency_type\":\"TAI_NAN\",\"visibility\":\"PRIVATE\",\"status\":\"ACCEPTED\","
                + "\"description\":\"Cần cứu hộ\",\"initial_lat\":10.723456,\"initial_lng\":106.654321}]}";
        ApiState<SosApiModels.RescuerMapResponse> result = await(rescuers.loadMap());
        assertEquals(ApiState.Status.SUCCESS, result.getStatus());
        Request sent = requests.poll();
        assertEquals("/api/rescuer/map", sent.url().encodedPath());
        assertEquals("Bearer initial-token", sent.header("Authorization"));
        List<SosRequest> data = result.getData().getData();
        assertEquals(2, data.size());
        assertEquals("PUBLIC", data.get(0).getVisibility());
        assertEquals(10.781234, com.javafinal.util.SosMapData.victim(data.get(0)).getLat(), 0.0000001);
        assertEquals("PRIVATE", data.get(1).getVisibility());
        assertEquals("ACCEPTED", data.get(1).getStatus());
        assertEquals("Cần cứu hộ", data.get(1).getDescription());
        assertEquals(106.654321, com.javafinal.util.SosMapData.victim(data.get(1)).getLng(), 0.0000001);
        session.clear();
        ApiState<SosApiModels.RescuerMapResponse> unauthenticated = rescuers.loadMap().getValue();
        assertEquals(ApiState.Status.ERROR, unauthenticated.getStatus());
        assertEquals(401, unauthenticated.getError().getStatusCode());
        assertTrue(requests.isEmpty());
    }

    @Test public void teamManagementUsesPhoneForAddAndMembershipIdsForChanges() throws Exception {
        responseBody = "{\"success\":true,\"data\":[]}";
        endpoint("GET", "/api/rescuer/teams/mine", true, null, teams::loadMyTeams);
        responseBody = "{\"success\":true,\"message\":\"Saved\",\"data\":{\"member_id\":\"m1\",\"role\":\"MEDIC\"}}";
        endpoint("POST", "/api/rescuer/teams/t1/members", true, "{\"phone\":\"+84901234567\",\"role\":\"MEDIC\"}",
                () -> teams.addMember("t1", " +84901234567 ", "MEDIC"));
        endpoint("PUT", "/api/rescuer/teams/t1/members/m1", true, "{\"role\":\"DRIVER\"}",
                () -> teams.updateMember("t1", "m1", "DRIVER"));
        responseBody = "{\"success\":true,\"message\":\"Deleted\",\"member_id\":\"m1\"}";
        endpoint("DELETE", "/api/rescuer/teams/t1/members/m1", true, null, () -> teams.deleteMember("t1", "m1"));
    }

    @Test public void teamPermissionsComeFromSelectedTeamAndRequireLeaderRole() throws Exception {
        responseBody = "{\"success\":true,\"data\":["
                + "{\"team_id\":\"t1\",\"current_user_role\":\"LEADER\",\"can_manage\":true,\"members\":["
                + "{\"member_id\":\"m1\",\"user_id\":\"u1\",\"name\":\"An\",\"phone\":\"0901234567\",\"role\":\"LEADER\"}]},"
                + "{\"team_id\":\"t2\",\"current_user_role\":\"MEMBER\",\"can_manage\":false},"
                + "{\"team_id\":\"t3\",\"current_user_role\":\"MEMBER\",\"can_manage\":true},"
                + "{\"team_id\":\"t4\",\"current_user_role\":\"LEADER\"}]}";
        ApiState<SosApiModels.RescueTeamsResponse> result = await(teams.loadMyTeams());
        assertEquals(ApiState.Status.SUCCESS, result.getStatus());
        List<RescueTeam> data = result.getData().getData();
        assertTrue(data.get(0).canManage());
        assertEquals("m1", data.get(0).getMembers().get(0).getMemberId());
        assertEquals("An", data.get(0).getMembers().get(0).getName());
        assertFalse(data.get(1).canManage());
        assertFalse(data.get(2).canManage());
        assertFalse(data.get(3).canManage());
    }

    @Test public void teamValidationAndForbiddenWritesDoNotGrantManagement() throws Exception {
        invalid(teams.addMember("t1", "invalid", "MEMBER"));
        invalid(teams.addMember("t1", "0901234567", "ADMIN"));
        invalid(teams.updateMember("t1", "m1", "ADMIN"));
        invalid(teams.deleteMember("t1", ""));
        assertTrue(requests.isEmpty());
        responseCode = 403;
        responseBody = "{\"detail\":\"Chỉ trưởng đội được quản lý đội viên.\"}";
        ApiState<TeamApiModels.MemberResponse> result = await(teams.updateMember("t1", "m1", "LEADER"));
        assertEquals(ApiState.Status.ERROR, result.getStatus());
        assertEquals(403, result.getError().getStatusCode());
        assertEquals("initial-token", session.getAccessToken());
    }

    private void invalid(LiveData<? extends ApiState<?>> state) {
        assertEquals(ApiState.Status.ERROR, state.getValue().getStatus());
        assertEquals(ApiError.Kind.VALIDATION, state.getValue().getError().getKind());
    }

    private <T> void endpoint(String method, String path, boolean authenticated, String body,
            Supplier<LiveData<ApiState<T>>> action) throws Exception {
        String expectedToken = session.getAccessToken();
        LiveData<ApiState<T>> live = action.get();
        assertEquals(ApiState.Status.LOADING, live.getValue().getStatus());
        ApiState<T> result = await(live);
        assertEquals(result.getError() == null ? path : result.getError().getMessage(),
                ApiState.Status.SUCCESS, result.getStatus());
        Request request = requests.poll();
        assertNotNull(path, request);
        assertEquals(method, request.method());
        assertEquals(path, request.url().encodedPath() + (request.url().encodedQuery() == null
                ? "" : "?" + request.url().encodedQuery()));
        assertEquals(authenticated ? "Bearer " + expectedToken : null, request.header("Authorization"));
        if (body == null) assertNull(request.body());
        else {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            assertEquals(JsonParser.parseString(body), JsonParser.parseString(buffer.readUtf8()));
            assertTrue(request.body().contentType().toString().startsWith("application/json"));
        }
    }

    private <T> ApiState<T> await(LiveData<ApiState<T>> state) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (state.getValue().getStatus() == ApiState.Status.LOADING && System.nanoTime() < deadline) {
            drainCallbacks();
            Thread.sleep(2);
        }
        assertNotEquals("Timed out waiting for API", ApiState.Status.LOADING, state.getValue().getStatus());
        return state.getValue();
    }

    private void waitForCallback() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (callbacks.isEmpty() && System.nanoTime() < deadline) Thread.sleep(2);
        assertFalse("Timed out waiting for callback", callbacks.isEmpty());
    }

    private void drainCallbacks() {
        Runnable callback;
        while ((callback = callbacks.poll()) != null) callback.run();
    }
}
