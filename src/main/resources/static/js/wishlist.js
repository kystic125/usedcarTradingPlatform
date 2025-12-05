/**
 * 찜하기 토글 함수
 * @param {number} vehicleId - 차량 ID
 * @param {HTMLElement} btnElement - 클릭한 버튼 요소 (this)
 */
function toggleFavorite(vehicleId, btnElement) {

    fetch('/favorites/toggle/' + vehicleId, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.text())
    .then(status => {
        if (status === 'login required') {
            if(confirm("로그인이 필요합니다. 이동하시겠습니까?")) {
                window.location.href = '/login';
            }
        } else if (status === 'added') {
            // 찜 성공: 빨간 하트 클래스 추가
            btnElement.classList.add('favorites-active');
            btnElement.setAttribute('title', 'Remove from favorites');
        } else if (status === 'removed') {
            // 찜 해제: 빨간 하트 클래스 제거
            btnElement.classList.remove('favorites-active');
            btnElement.setAttribute('title', 'Add to favorites');

            // 만약 '내 찜 목록' 페이지라면, 화면에서 카드를 바로 삭제
            if (window.location.pathname.includes('/favorites')) {
                const cardCol = btnElement.closest('.col-sm-6');
                if (cardCol) {
                    cardCol.style.transition = "opacity 0.5s";
                    cardCol.style.opacity = "0";
                    setTimeout(() => cardCol.remove(), 500);
                }
            }
        }
    })
    .catch(err => console.error('Wishlist Error:', err));
}