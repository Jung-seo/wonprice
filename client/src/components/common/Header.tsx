import React, { useEffect, useRef } from "react";
import { useRecoilValue, useSetRecoilState } from "recoil";
import { headerHeightState, loginState } from "../../atoms/atoms"; // loginState 추가
import HeaderLogin from "../header/HeaderLogin"; // 로그인 시 표시할 헤더
import HeaderLogout from "../header/HeaderLogout"; // 로그아웃 시 표시할 헤더

const MainHeader: React.FC = () => {
  // 헤더 높이 구하기
  const headerRef = useRef<HTMLDivElement | null>(null);
  const setHeaderHeight = useSetRecoilState(headerHeightState);

  useEffect(() => {
    if (headerRef.current !== null) {
      setHeaderHeight(headerRef.current?.clientHeight);
    }
  }, [setHeaderHeight]);

  // 로그인 상태 확인
  const isLoggedIn = useRecoilValue(loginState); // loginState 사용

  return <div ref={headerRef}>{isLoggedIn ? <HeaderLogin /> : <HeaderLogout />}</div>;
};

export default MainHeader;
