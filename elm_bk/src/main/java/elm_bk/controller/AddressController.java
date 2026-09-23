package elm_bk.controller;

import elm_bk.dto.AddressCreateDTO;
import elm_bk.result.HttpResult;
import elm_bk.service.AddressService;
import elm_bk.vo.AddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addresses;
    @GetMapping("/me") public HttpResult<List<AddressVO>> list() { return HttpResult.success(addresses.list()); }
    @PostMapping({"", "/me"}) public HttpResult<AddressVO> create(@Valid @RequestBody AddressCreateDTO request) {
        return HttpResult.success(addresses.create(request));
    }
    @GetMapping("/{id}") public HttpResult<AddressVO> get(@PathVariable Long id) { return HttpResult.success(addresses.get(id)); }
    @PutMapping("/{id}") public HttpResult<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressCreateDTO request) {
        return HttpResult.success(addresses.update(id, request));
    }
    @DeleteMapping("/{id}") public HttpResult<Void> delete(@PathVariable Long id) {
        addresses.delete(id); return HttpResult.success(null);
    }
    @PutMapping("/{id}/default") public HttpResult<AddressVO> setDefault(@PathVariable Long id) {
        return HttpResult.success(addresses.setDefault(id));
    }
}
